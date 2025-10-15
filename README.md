🧱 Core Spring Boot Dependencies

spring-boot-starter	
spring-boot-starter-web	
spring-boot-starter-data-jpa	
spring-boot-starter-security	
spring-boot-starter-validation	

🗄️ Database Dependencies
postgresql	
pgvector (extension)  Enables vector-based similarity search for recommendation embeddings.


🧩 Utility & Supporting Dependencies
lombok	
com.auth0:java-jwt	
jakarta.servlet:jakarta.servlet-api	

⚙️ Build Tool Plugins
spring-boot-maven-plugin

🐍 Python-side Dependencies
psycopg2-binary	           PostgreSQL adapter for Python to insert data and vectors.
sentence-transformers	   Generates semantic embeddings from text data (titles, subjects, authors).
tqdm	                   Adds progress bars during CSV import or embedding generation.


🧰 System Requirements
Java	17 or later
Maven	3.8+
PostgreSQL	18+
pgvector extension	0.5.0+
Python (optional)	3.12.3