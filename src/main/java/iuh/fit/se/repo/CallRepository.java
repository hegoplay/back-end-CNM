package iuh.fit.se.repo;

import java.time.LocalDateTime;
import java.util.Optional;

import iuh.fit.se.model.Call;
import iuh.fit.se.model.enumObj.CallStatus;
import iuh.fit.se.model.enumObj.CallType;

public interface CallRepository {

    // Tạo document trong `calls`

	void createCall(Call call);
	
    Optional<Call> findById(String callId);
    
    void deleteById(String id);
	
	boolean existsById(String id);
    
	void updateCall(Call call);
	
    // Cập nhật trạng thái cuộc gọi
    
}