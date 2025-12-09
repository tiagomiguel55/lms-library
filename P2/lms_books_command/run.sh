# Check if the node is already part of a swarm
SWARM_STATUS=$(docker info --format '{{.Swarm.LocalNodeState}}' 2>/dev/null)

if [ "$SWARM_STATUS" != "active" ]; then
    echo "Node is not in a swarm. Initializing swarm..."
    docker swarm init
fi

## Ensure overlay network exists before starting containers
if ! docker network ls --filter name=^lms_overlay_attachable_network$ --format '{{.Name}}' | grep -q '^lms_overlay_attachable_network$'; then
  docker network create --driver=overlay --attachable lms_overlay_attachable_network 2>/dev/null
fi
if [[ $1 =~ ^-?[0-9]+$ ]]; then
  # It's a valid integer (including negatives)
  if (( $1 < 1 )); then
    ./shutdown.sh
    exit
  fi
else
  echo "Error: Argument is not a valid number"
  exit
fi

db_base_name="books_db_"
db_base_port=55000

latest_i=$(docker ps --filter "name=^${db_base_name}[1-9][0-9]*$" --format "{{.Names}}" | sort -V | tail -n 1 | grep -oE '[0-9]+$' 2>/dev/null)

if((latest_i > $1)); then
  for ((i = $1+1; i <= latest_i; i++)); do
    db_name="$db_base_name${i}"
    db_port=$(($db_base_port + i))

    docker stop ${db_name} 1>/dev/null
    docker rm ${db_name} 1>/dev/null

    echo "Stopped ${db_name} on port ${db_port}"
  done

else
  if ((latest_i < $1)); then

    for ((i = latest_i+1; i <= $1; i++)); do
      db_name="$db_base_name${i}"
      db_port=$((db_base_port + i))

      docker run -d \
        --name "${db_name}" \
        --network lms_overlay_attachable_network \
        -e POSTGRES_USER=postgres \
        -e POSTGRES_PASSWORD=password \
        postgres

      echo "Started ${db_name} on port ${db_port}"
    done
  fi
fi

echo Running $1 instances of lmsbooks, each connecting to a different/specific Postgres DBMS

if docker service ls --filter "name=lmsbooks" --format "{{.Name}}" | grep -q "^lmsbooks$"; then
  docker service scale lmsbooks=$1
else
  docker service create -d \
    --name lmsbooks \
    --env spring.datasource.url=jdbc:postgresql://books_db_{{.Task.Slot}}:5432/postgres \
    --env SPRING_PROFILES_ACTIVE=bootstrap \
    --env spring.datasource.username=postgres \
    --env spring.datasource.password=password \
    --env file.upload-dir=/tmp/uploads-psoft-g1-instance{{.Task.Slot}} \
    --env spring.rabbitmq.host=rabbitmq \
    --mount type=volume,source=uploaded_files_volume_{{.Task.Slot}},target=/tmp \
    --publish 8087:8080 \
    --network lms_overlay_attachable_network \
    lmsbooks:latest

  docker service scale lmsbooks=$1
fi