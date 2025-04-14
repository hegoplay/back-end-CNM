package iuh.fit.se.errorHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	// Xử lý ngoại lệ chung (Exception)
	@ExceptionHandler(Exception.class)
	protected ResponseEntity<ProblemDetail> handleEntityNotFoundException(Exception ex, HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				ex.getMessage());
		problemDetail.setTitle("Internal Server Error");
		log.error("Internal Server Error: {}", ex.getMessage());
		return ResponseEntity.of(problemDetail).build();
	}

	@ExceptionHandler(DynamoDbException.class)
	protected ResponseEntity<ProblemDetail> handleDynamoDbException(DynamoDbException ex, HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				ex.getMessage());
		problemDetail.setTitle("DynamoDB Error");
		return ResponseEntity.of(problemDetail).build();
	}
	// Xử lý ngoại lệ cụ thể khi không tìm thấy đối tượng
	@ExceptionHandler(RuntimeException.class)
	protected ResponseEntity<ProblemDetail> handleRuntimeExceptionhandleRuntimeException(RuntimeException ex, HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				ex.getMessage());
		problemDetail.setTitle("Runtime Exception");
		return ResponseEntity.of(problemDetail).build();
	}

	// Xử lý ngoại lệ cụ thể khi không tìm thấy đối tượng
	@ExceptionHandler(IllegalArgumentException.class)
	protected ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex,
			HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				ex.getMessage());
		problemDetail.setTitle("Illegal Argument Exception");
		log.info("Illegal Argument Exception: {}", ex.getMessage());
		return ResponseEntity.of(problemDetail).build();
	}
	
	// Xử lý ngoại lệ cụ thể khi xác thực thất bại
	@ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
	public ResponseEntity<String> handleAuthenticationException() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sai số điện thoại hoặc mật khẩu");
	}
	
//	@ExceptionHandler(UnauthorizedException.class)
//    public ResponseEntity<Map<String, Object>> handleUnauthorizedException(UnauthorizedException ex) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", false);
//        response.put("error", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
//    }
//
//    // Xử lý Forbidden (403)
//    @ExceptionHandler(ForbiddenException.class)
//    public ResponseEntity<Map<String, Object>> handleForbiddenException(ForbiddenException ex) {
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", false);
//        response.put("error", ex.getMessage());
//        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
//    }
}
