package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.AttendanceService.AbsencePattern;
import com.littlehorizons.erp.util.ConsoleFormat;
import com.littlehorizons.erp.util.ConsoleInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Console screens for general reports and institution statistics.
 */
public class ReportView {

    private static final String SEPARATOR = "-".repeat(110);

    private final ConsoleInput input;

    public ReportView(ConsoleInput input) {
        this.input = input;
    }

    public int showMenuAndReadOption() {
        System.out.println();
        System.out.println("==============================");
        System.out.println("   REPORTES Y ESTADISTICAS");
        System.out.println("==============================");
        System.out.println("1. Reporte de inasistencias criticas (>3 ausencias por semana)");
        System.out.println("2. Reporte de estudiantes morosos (pagos de pension pendientes)");
        System.out.println("3. Reporte de prestamos de recursos vencidos");
        System.out.println("4. Resumen estadistico general del colegio");
        System.out.println("0. Volver al menu principal");
        return input.readMenuOption("Seleccione una opcion: ");
    }

    public LocalDate askStartDate() {
        return input.readDateKeepingCurrent("Fecha inicial", LocalDate.now().minusWeeks(4));
    }

    public LocalDate askEndDate() {
        return input.readDateKeepingCurrent("Fecha final", LocalDate.now());
    }

    public int askAcademicYear() {
        return input.readIntKeepingCurrent("Ano academico", LocalDate.now().getYear(), 2000, 2100);
    }

    public String askAcademicPeriod() {
        return input.readChoice("Periodo academico", List.of("I", "II", "III", "IV"));
    }

    public void showAbsenceReport(List<AbsencePattern> patterns) {
        System.out.println(SEPARATOR);
        System.out.println("REPORTE DE INASISTENCIAS PREOCUPANTES (>3 ausencias en una semana)");
        System.out.println(SEPARATOR);
        if (patterns.isEmpty()) {
            System.out.println("No se detectaron estudiantes con inasistencias criticas en el rango indicado.");
            return;
        }
        System.out.printf("%-6s %-14s %-32s %-16s %-10s%n",
                "ID", "DOCUMENTO", "ESTUDIANTE", "SEMANA DEL", "AUSENCIAS");
        System.out.println(SEPARATOR);
        for (AbsencePattern p : patterns) {
            Student s = p.student();
            System.out.printf("%-6d %-14s %-32s %-16s %-10d%n",
                    s.getId(),
                    s.getDocumentType() + " " + s.getDocumentNumber(),
                    ConsoleFormat.fit(s.getFullName(), 32),
                    p.weekStart().toString(),
                    p.absences());
        }
        System.out.println(SEPARATOR);
        System.out.println("Total registros de alerta: " + patterns.size());
    }

    public void showDelinquentReport(List<Student> students, int academicYear, String period) {
        System.out.println(SEPARATOR);
        System.out.printf("REPORTE DE MOROSIDAD (Periodo %s - Ano %d)%n", period, academicYear);
        System.out.println(SEPARATOR);
        if (students.isEmpty()) {
            System.out.println("Todos los estudiantes se encuentran al dia con sus pagos.");
            return;
        }
        System.out.printf("%-6s %-14s %-32s %-20s %-12s%n",
                "ID", "DOCUMENTO", "ESTUDIANTE", "TELEFONO", "ESTADO");
        System.out.println(SEPARATOR);
        for (Student s : students) {
            System.out.printf("%-6d %-14s %-32s %-20s %-12s%n",
                    s.getId(),
                    s.getDocumentType() + " " + s.getDocumentNumber(),
                    ConsoleFormat.fit(s.getFullName(), 32),
                    ConsoleFormat.fit(s.getPhone(), 20),
                    s.getStatus());
        }
        System.out.println(SEPARATOR);
        System.out.println("Total estudiantes morosos: " + students.size());
    }

    public void showOverdueLoansReport(List<Loan> overdueLoans) {
        System.out.println(SEPARATOR);
        System.out.println("REPORTE DE PRESTAMOS VENCIDOS");
        System.out.println(SEPARATOR);
        if (overdueLoans.isEmpty()) {
            System.out.println("No hay prestamos vencidos. Todos los recursos estan al dia.");
            return;
        }
        System.out.printf("%-10s %-24s %-26s %-12s %-12s %-10s%n",
                "COD RECURSO", "RECURSO", "SOLICITANTE", "TIPO", "F. VENCE", "DIAS ATRASO");
        System.out.println(SEPARATOR);
        LocalDate today = LocalDate.now();
        for (Loan l : overdueLoans) {
            System.out.printf("%-10s %-24s %-26s %-12s %-12s %-10d%n",
                    ConsoleFormat.fit(l.getResource().getCode(), 10),
                    ConsoleFormat.fit(l.getResource().getName(), 24),
                    ConsoleFormat.fit(l.getBorrowerName(), 26),
                    l.getBorrowerType().label(),
                    l.getDueDate(),
                    l.getDaysOverdue(today));
        }
        System.out.println(SEPARATOR);
        System.out.println("Total prestamos vencidos: " + overdueLoans.size());
    }

    public void showInstitutionSummary(Map<String, Object> stats) {
        System.out.println(SEPARATOR);
        System.out.println("RESUMEN INSTITUCIONAL - COLEGIO INTERNACIONAL LITTLE HORIZONS");
        System.out.println(SEPARATOR);
        printStat("Estudiantes activos", stats.get("activeStudents"));
        printStat("Estudiantes graduados", stats.get("graduatedStudents"));
        printStat("Profesores activos", stats.get("activeTeachers"));
        printStat("Materias ofrecidas", stats.get("totalSubjects"));
        printStat("Clubes activos", stats.get("activeClubs"));
        printStat("Eventos programados", stats.get("totalEvents"));
        printStat("Recursos en inventario", stats.get("totalResources"));
        printStat("Prestamos activos", stats.get("activeLoans"));
        printStat("Prestamos vencidos", stats.get("overdueLoans"));
        Object totalRevenue = stats.get("totalRevenue");
        if (totalRevenue != null) {
            printStat("Recaudo historico total", "$" + ((BigDecimal) totalRevenue).toPlainString());
        }
        System.out.println(SEPARATOR);
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showError(String message) {
        input.showError(message);
    }

    private void printStat(String label, Object value) {
        System.out.printf("%-35s: %s%n", label, value == null ? "0" : value.toString());
    }
}
