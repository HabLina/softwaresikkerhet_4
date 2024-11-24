package IKT222.patients;

import java.util.HashMap;
import java.util.Map;

public class LoginService {
    // Set the maximum allowed attempts
    private static final int MAX_ATTEMPTS = 5;
    // Set the lock time in milliseconds to 5 minutes
    private static final long LOCK_TIME = 5 * 60 * 1000;
  
    // Map the username to the login attempt information
    private Map<String, LoginAttempt> attempts = new HashMap<>();
    
    // Function to check if the account is locked by checking the attempt history
    public boolean isLocked(String username) {

        // Get the ammount of attempts for the username
        LoginAttempt attempt = attempts.get(username);
        if (attempt != null && attempt.isLocked()) {

            // Check that the ammount 
            if (System.currentTimeMillis() - attempt.getLockTime() > LOCK_TIME) {

                // Unlock the account after the lock period
                attempt.reset();
                return false;
            }
            return true;
        }
        return false;
    }
  
    // Record all failed login attempts
    public void recordFailedAttempt(String username) {

        // Get the LoginAttempts object for the user and increments the failed attepmts
        LoginAttempt attempt = attempts.getOrDefault(username, new LoginAttempt());
        attempt.increment();

        // Lock the LoginAttempt object if the number of failed attempts is bigger or equal to MAX_ATTEMPTS
        if (attempt.getAttempts() >= MAX_ATTEMPTS) {
            attempt.lock();
        }

        // Update the attempts map for the given user
        attempts.put(username, attempt);
    }
  
    // Function to clear the failed login attempts by removing the users enty from the attempts map
    public void resetAttempts(String username) {
        attempts.remove(username);
    }
  
    // Class to log each login attempt for a user
    private static class LoginAttempt {
        private int attempts;
        private long lockTime;
        private boolean locked;
  
        // Functions used in LoginService
        public void increment() {
            attempts++;
        }
  
        public void lock() {
            locked = true;
            lockTime = System.currentTimeMillis();
        }
  
        public boolean isLocked() {
            return locked;
        }
  
        public int getAttempts() {
          return attempts;
        }
  
        public long getLockTime() {
            return lockTime;
        }
  
        public void reset() {
            attempts = 0;
            locked = false;
            lockTime = 0;
        }
    }
  }