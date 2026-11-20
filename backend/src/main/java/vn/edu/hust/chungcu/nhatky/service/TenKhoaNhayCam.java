package vn.edu.hust.chungcu.nhatky.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Quy tắc tên khóa nhạy cảm của chi tiết nhật ký (spec 0007, AC-8 b): tách tên theo ranh
 * giới camelCase và dấu {@code _}, đổi về chữ thường; nhạy cảm khi có một từ trong
 * {@link #TU_NHAY_CAM} hoặc hai từ liền nhau {@code mat}, {@code khau}.
 */
public final class TenKhoaNhayCam {

	static final Set<String> TU_NHAY_CAM = Set.of("password", "token", "tokens", "hash", "secret", "cookie", "otp",
			"matkhau");

	private static final String RANH_GIOI_CAMEL = "(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])";

	private TenKhoaNhayCam() {
	}

	public static boolean nhayCam(String ten) {
		List<String> tu = tach(ten);
		for (int i = 0; i < tu.size(); i++) {
			if (TU_NHAY_CAM.contains(tu.get(i))) {
				return true;
			}
			if (tu.get(i).equals("mat") && i + 1 < tu.size() && tu.get(i + 1).equals("khau")) {
				return true;
			}
		}
		return false;
	}

	static List<String> tach(String ten) {
		List<String> tu = new ArrayList<>();
		for (String doan : ten.split("_")) {
			for (String phan : doan.split(RANH_GIOI_CAMEL)) {
				if (!phan.isEmpty()) {
					tu.add(phan.toLowerCase(Locale.ROOT));
				}
			}
		}
		return tu;
	}

}
