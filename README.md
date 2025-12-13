 Stock Tracker Backend

**What it is**  
A Spring Boot backend that fetches stock prices, company information, and historical data using a rate-limited external API, while storing users’ favourite stocks.

**What it does**
- Fetches live stock data from external **Alpha Vantage API**
 
- Returns company overview ->**(market cap, sector, P/E ratio)**
 
- Provides **(daily/weekly/monthly)** historical stock data
 
- Stores and retrieves **favourite stocks** from a database (h2)
 
- Handles strict API limits **(25 calls/day)** through intelligent caching strategies.

**Why it matters**  
Built with real world constraints in mind (limited API calls, error handling, clean architecture), not just basic CRUD operations.

------

 🛠️ Tech Used

- **Java**
- **Spring Boot**
- **Spring MVC** (REST APIs)  
- Spring Data JPA (Hibernate)  
- H2 Database (file-based)  
- Spring WebClient (external API integration)  
- **Alpha Vantage API** (stock market data)  
- **Maven** (build & dependency management)  
- Jackson (JSON serialization/deserialization)  
- Lombok (reducing boilerplate code)  
- **Caching strategies to handle strict API limits**

-> Reliable  
-> Production-style backend structure  
-> Portfolio & interview-ready
