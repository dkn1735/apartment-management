package vn.edu.hust.chungcu.auth.service;

import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import vn.edu.hust.chungcu.auth.dto.ChiTietVaiTro;
import vn.edu.hust.chungcu.auth.dto.PhamVi;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.phanquyen.VaiTroTrongToken;
import vn.edu.hust.chungcu.auth.repository.VaiTroQuery;

/**
 * Phạm vi dữ liệu của người đang gọi qua HTTP (spec 0006, AC-6). Mọi phân hệ gọi service
 * này rồi lọc theo {@link PhamViTruyCap}; kết quả nhớ trong request hiện tại.
 */
@Service
public class PhamViTruyCapService {

	private static final String ATTRIBUTE = PhamViTruyCapService.class.getName() + ".phamVi";

	private final VaiTroQuery vaiTroQuery;

	private final PhamViTinhToan tinhToan;

	public PhamViTruyCapService(VaiTroQuery vaiTroQuery, PhamViTinhToan tinhToan) {
		this.vaiTroQuery = vaiTroQuery;
		this.tinhToan = tinhToan;
	}

	/**
	 * @throws IllegalStateException khi không ở trong một request đã xác thực bằng access
	 * token (job, luồng chạy nền)
	 */
	public PhamViTruyCap cuaNguoiDangGoi() {
		RequestAttributes request = RequestContextHolder.getRequestAttributes();
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (request == null || !(authentication instanceof JwtAuthenticationToken jwt)) {
			throw new IllegalStateException("Phạm vi truy cập chỉ có nghĩa trong một request đã xác thực");
		}
		if (request.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) instanceof PhamViTruyCap daTinh) {
			return daTinh;
		}
		PhamViTruyCap phamVi = tinh(jwt.getName(), VaiTroTrongToken.cua(jwt));
		request.setAttribute(ATTRIBUTE, phamVi, RequestAttributes.SCOPE_REQUEST);
		return phamVi;
	}

	private PhamViTruyCap tinh(String maNguoiDung, Set<VaiTro> vaiTro) {
		String maCuDan = null;
		if (vaiTro.contains(VaiTro.CU_DAN)) {
			ChiTietVaiTro.VaiTroCuDan cuDan = this.vaiTroQuery.cua(maNguoiDung).chiTiet().cuDan();
			maCuDan = (cuDan != null) ? cuDan.maCuDan() : null;
		}
		PhamVi phamVi = this.tinhToan.tinh(vaiTro, maCuDan);
		return new PhamViTruyCap(maNguoiDung, vaiTro, phamVi.maToaNha(), phamVi.maCanHo());
	}

}
