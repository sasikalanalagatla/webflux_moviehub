# Movie Review Application Setup

## Prerequisites

1. Java 21 or higher
2. MongoDB running on localhost:27017
3. TMDB API Key

## TMDB API Key Setup

1. Go to [The Movie Database (TMDB)](https://www.themoviedb.org/)
2. Create an account or log in
3. Go to your account settings → API
4. Request an API key
5. Set the environment variable:

### On Linux/macOS:
```bash
export TMDB_API_KEY=your_actual_api_key_here
```

### On Windows:
```cmd
set TMDB_API_KEY=your_actual_api_key_here
```

### Using IntelliJ IDEA:
1. Go to Run → Edit Configurations
2. Select your application configuration
3. In Environment variables, add:
   - Name: `TMDB_API_KEY`
   - Value: `your_actual_api_key_here`

## Running the Application

1. Make sure MongoDB is running
2. Set the TMDB_API_KEY environment variable
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

## Troubleshooting

If you get the error "Could not resolve placeholder 'TMDB_API_KEY'", make sure:
1. You have set the TMDB_API_KEY environment variable
2. The environment variable is available to your IDE/terminal
3. You have restarted your IDE after setting the environment variable