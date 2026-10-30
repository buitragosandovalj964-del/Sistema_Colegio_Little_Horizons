package test.dao;

import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.Teacher;
import java.sql.SQLException;
import java.util.Optional;

public class profeDAO {

    private final TeacherDao teacherDao = new TeacherDao();

    public Optional<Teacher> findById(int id) throws SQLException {
        return teacherDao.findById(id);
    }

    public boolean deleteById(int id) throws SQLException {
        return teacherDao.deleteById(id);
    }
}