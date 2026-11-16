package vn.edu.hust.chungcu.canho.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.hust.chungcu.canho.entity.ToaNha;

public interface ToaNhaRepository extends JpaRepository<ToaNha, String> {

	@Query("select t.maToaNha from ToaNha t order by t.maToaNha")
	List<String> findAllMaToaNha();

}
