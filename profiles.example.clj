{:dev {:env {:app-env              "development"
             :port                 "8080"
             :db-url               "jdbc:postgresql://localhost:5432/revenu_dev"
             :db-user              "your-db-user"
             :db-password          "your-db-password"
             :google-client-id     "your-google-client-id.apps.googleusercontent.com"
             :google-client-secret "your-google-client-secret"
             :google-redirect-uri  "http://localhost:8080/auth/callback"
             :jwt-secret           "a-random-secret-at-least-32-chars-long"}}}
