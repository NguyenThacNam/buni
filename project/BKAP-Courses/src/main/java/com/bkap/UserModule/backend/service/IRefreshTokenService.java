package com.bkap.UserModule.backend.service;

import java.util.Optional;

import com.bkap.UserModule.backend.service.RefreshTokenService.KetQuaLamMoi;
import com.bkap.UserModule.entity.User;

public interface IRefreshTokenService {

	/** Cấp refresh token mới cho người dùng, trả về token THÔ để gửi cho client. */
	String cap(User user);

	/**
	 * Kiểm tra token còn dùng được không. Nếu được thì thu hồi nó và cấp token
	 * mới (xoay vòng), trả về kèm chủ nhân. Không hợp lệ thì trả Optional rỗng.
	 */
	Optional<KetQuaLamMoi> xacMinhVaXoay(String tokenTho);

	/** Thu hồi toàn bộ refresh token còn hiệu lực — dùng khi đăng xuất. */
	void thuHoiTatCa(User user);
}
