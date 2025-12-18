Context
In a previous sprint/project, the LMS application was developed, and configurability issues
were addressed.
While this application is functional, it was observed that its centralized (monolithic or
modular monolith) architecture hampers its performance, availability, scalability and
elasticity.

Goal
The goal of this sprint/project is to reengineer the LMS application by adopting a
decentralized/distributed architecture.
Requirements
Non-functional requirements
• The system must improve its availability.
• The system must increase the performance by 25% when in high demand (i.e. >Y
requests/period).
• The system must use hardware parsimoniously, according to the run time demanding of
the system. Demanding peaks of >Y requests/period occur seldom.
• The soQware clients should not be affected by the changes (if any) in the API, except in
extreme cases.
• The system must adhere to the company’s SOA strategy of API-led connectivity.
Functional requirements
• Student A: As a librarian, I want to create a Book, Author and Genre in the same process.
• Student B: As a librarian, I want to create a Reader and the respective User in the same
request.
• Student C: As a reader, upon returning a Book, I want to leave a text comment about the
Book and grading it (0-10).