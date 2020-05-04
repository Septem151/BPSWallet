
package bpswallet.crypto;

public class InvalidPasswordException extends Exception {
    public InvalidPasswordException(String message) {
        super(message);
    }
    
    public InvalidPasswordException() {
        super();
    }
}
