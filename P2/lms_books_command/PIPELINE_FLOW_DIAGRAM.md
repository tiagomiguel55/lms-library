# Jenkins Pipeline Flow Diagram

## Complete Pipeline Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         JENKINS PIPELINE START                               │
│                     (Select: dev, staging, or prod)                          │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │   1. Initialization           │
                    │   - Display build info        │
                    │   - Set environment vars      │
                    └───────────────┬───────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │   2. Environment Check        │
                    │   - Java 17                   │
                    │   - Maven                     │
                    │   - Docker                    │
                    └───────────────┬───────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │   3. Checkout from Git        │
                    │   - Clone repository          │
                    └───────────────┬───────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │   4. Build & Compile          │
                    │   - mvn clean compile         │
                    └───────────────┬───────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │ DEV ONLY                  │                           │
        ▼                           │                           │
┌───────────────┐                   │                           │
│ 5. Unit Tests │                   │                           │
│ - JUnit       │                   │                           │
│ - Publish     │                   │                           │
└───────┬───────┘                   │                           │
        │                           │                           │
        ▼                           │                           │
┌───────────────┐                   │                           │
│ 6. Mutation   │                   │                           │
│    Tests      │                   │                           │
│ - PITest      │                   │                           │
│ - HTML Report │                   │                           │
└───────┬───────┘                   │                           │
        │                           │                           │
        └───────────────────────────┼───────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │   7. Package                  │
                    │   - Create JAR file           │
                    └───────────────┬───────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    │   8. Build Docker Image       │
                    │   - Tag: build-number         │
                    │   - Tag: environment          │
                    │   - Tag: latest               │
                    └───────────────┬───────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼ DEV                       ▼ STAGING                  ▼ PROD
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│ 9a. Deploy Dev  │      │ 9b. Deploy      │      │ 9c. Production  │
│                 │      │     Staging     │      │     Workflow    │
│ ✅ AUTOMATIC    │      │                 │      │                 │
│                 │      │ ✅ AUTOMATIC    │      │ ⚠️  MANUAL      │
│ Single instance │      │                 │      │     APPROVAL    │
│ Port: 8081      │      │ Single instance │      │                 │
│                 │      │ Port: 8082      │      └────────┬────────┘
└─────────────────┘      └─────────────────┘               │
                                                            │
                                            ┌───────────────┴────────────────┐
                                            │ Email Notification Sent        │
                                            │ - Build details                │
                                            │ - Approval link                │
                                            │ - 30 min timeout               │
                                            └───────────────┬────────────────┘
                                                            │
                                            ┌───────────────┴────────────────┐
                                            │ Wait for Manual Approval       │
                                            │ Approver reviews and decides:  │
                                            │ ✅ Approve  or  ❌ Reject      │
                                            └───────────────┬────────────────┘
                                                            │
                                                    Approved │
                                                            ▼
                                            ┌────────────────────────────────┐
                                            │ 10. ROLLING UPDATE DEPLOYMENT  │
                                            │                                │
                                            │ Step 1: Update Instance 1      │
                                            │   - Stop lmsbooks_prod_1       │
                                            │   - Start new version          │
                                            │   - Health check (60s max)     │
                                            │   ✅ Wait 10s                  │
                                            │                                │
                                            │ Step 2: Update Instance 2      │
                                            │   - Stop lmsbooks_prod_2       │
                                            │   - Start new version          │
                                            │   - Health check (60s max)     │
                                            │   ✅ Wait 10s                  │
                                            │                                │
                                            │ Step 3: Update Instance 3      │
                                            │   - Stop lmsbooks_prod_3       │
                                            │   - Start new version          │
                                            │   - Health check (60s max)     │
                                            │   ✅ Complete                  │
                                            │                                │
                                            │ Ports: 8083, 8084, 8085        │
                                            └────────────────┬───────────────┘
                                                            │
        ┌───────────────────────────────────────────────────┴───────────────┐
        │                                                                    │
        │                   11. Post-Deployment Verification                │
        │                   - Check service status                          │
        │                   - View logs                                     │
        │                   - Verify health endpoints                       │
        │                                                                    │
        └───────────────────────────────┬────────────────────────────────────┘
                                        │
                        ┌───────────────┴───────────────┐
                        │   12. Cleanup Old Images      │
                        │   - Keep last 5 builds        │
                        │   - Remove older images       │
                        └───────────────┬───────────────┘
                                        │
        ┌───────────────────────────────┴────────────────────────────────┐
        │                                                                 │
        ▼ SUCCESS                                           FAILURE ▼     │
┌──────────────────┐                                  ┌──────────────────┐
│ Email: SUCCESS   │                                  │ Email: FAILURE   │
│ - Environment    │                                  │ - Error details  │
│ - Build number   │                                  │ - Console link   │
│ - Image tag      │                                  └──────────────────┘
└──────────────────┘

                        PIPELINE COMPLETE
```

## Environment-Specific Paths

### DEV Environment Path
```
Checkout → Compile → Unit Tests → Mutation Tests → Package → 
Build Image → Deploy Automatically → Verify → Complete
```

### STAGING Environment Path
```
Checkout → Compile → Package → Build Image → 
Deploy Automatically → Verify → Complete
```

### PRODUCTION Environment Path
```
Checkout → Compile → Package → Build Image → 
Email Notification → Manual Approval (Wait) → 
Rolling Update (Instance 1 → Instance 2 → Instance 3) → 
Verify → Complete
```

## Rolling Update Detail

```
Production has 3 instances running simultaneously:

Initial State:
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Instance 1   │  │ Instance 2   │  │ Instance 3   │
│   v1.0       │  │   v1.0       │  │   v1.0       │
│   :8083      │  │   :8084      │  │   :8085      │
│   ✅ Running │  │   ✅ Running │  │   ✅ Running │
└──────────────┘  └──────────────┘  └──────────────┘

Step 1: Update Instance 1
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Instance 1   │  │ Instance 2   │  │ Instance 3   │
│   v2.0       │  │   v1.0       │  │   v1.0       │
│   :8083      │  │   :8084      │  │   :8085      │
│   ✅ Running │  │   ✅ Running │  │   ✅ Running │
└──────────────┘  └──────────────┘  └──────────────┘
                   ↑ Still serving traffic ↑

Step 2: Update Instance 2
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Instance 1   │  │ Instance 2   │  │ Instance 3   │
│   v2.0       │  │   v2.0       │  │   v1.0       │
│   :8083      │  │   :8084      │  │   :8085      │
│   ✅ Running │  │   ✅ Running │  │   ✅ Running │
└──────────────┘  └──────────────┘  └──────────────┘
 ↑ Serving v2.0   ↑                   ↑ Still v1.0

Step 3: Update Instance 3 (Final)
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Instance 1   │  │ Instance 2   │  │ Instance 3   │
│   v2.0       │  │   v2.0       │  │   v2.0       │
│   :8083      │  │   :8084      │  │   :8085      │
│   ✅ Running │  │   ✅ Running │  │   ✅ Running │
└──────────────┘  └──────────────┘  └──────────────┘
        All instances now running v2.0 ✅
```

## Key Features

✅ **Zero Downtime**: Always 2-3 instances running during update
✅ **Health Checks**: Each instance verified before proceeding
✅ **Gradual Rollout**: Issues detected early, only affects one instance
✅ **Auto Rollback**: If health check fails, deployment stops
✅ **Manual Approval**: Production requires human verification
✅ **Email Notifications**: Alerts sent for approvals and status

