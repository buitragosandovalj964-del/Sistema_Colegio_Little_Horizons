# Little Horizons ERP — Sistema de Gestion Educativa y Administrativa

Sistema de Planificacion de Recursos Empresariales (ERP) integral desarrollado en Java para el Colegio Internacional "Little Horizons", orientado a centralizar y automatizar los procesos academicos, administrativos, financieros y de gestion de recursos.

---

## 1. Titulo del Proyecto
**Sistema ERP Little Horizons (PryLittleHorizonsERP)**  
*Colegio Internacional Little Horizons S.A.S.*

---

## 2. Descripcion

### Resumen del Proposito
El Colegio Internacional "Little Horizons" atiende a mas de 500 estudiantes desde preescolar hasta grado 11°. Este software centraliza toda la informacion institucional en una unica base de datos relacional y proporciona una aplicacion de consola profesional con arquitectura en capas (MVC + DAO + Service Layer).

### Problemas que Resuelve
1. **Perdida de Informacion Academica:** Sustituye los cuadernos fisicos por registros academicos inmutables en base de datos con control de periodos (I a IV y Final) y promedios calculados automaticamente.
2. **Desorganizacion en Actividades Extracurriculares:** Administra catalogos de clubes con cupos maximos validados y seguimiento de participantes y posiciones en eventos competitivos.
3. **Caos en la Facturacion y Cobros:** Automatiza la generacion de recibos de pago, identificacion inmediata de estudiantes morosos y emision de facturas electronicas en formato PDF bajo los estandares de la DIAN (CUFE y QR simulados).
4. **Desperdicio de Recursos e Inventario:** Controla prestamos de libros, equipos de laboratorio y dispositivos tecnologicos para estudiantes y profesores con control estricto de fechas de vencimiento y bloqueos por mora.
5. **Comunicacion y Control de Asistencia:** Registra asistencia diaria y genera alertas automaticas de ausentismo cronico (>3 ausencias por semana).

### Funcionalidades Principales
- **Gestion de Estudiantes y Acudientes:** CRUD completo, identificacion por documento, vinculacion familiar y responsables financieros.
- **Gestion Academica:** Matriculas por grado, asignacion de carga docente, inscripcion a materias, control de calificaciones y asistencia diaria.
- **Actividades Extracurriculares:** Gestion de clubes con aforo controlado y eventos institucionales con roles y posiciones.
- **Gestion Financiera:** Conceptos de pago, registro de pagos con recibo unico, historial y generacion de facturas electronicas en PDF.
- **Gestion de Recursos:** Inventario categorizado, prestamos independientes para estudiantes y docentes, devoluciones y seguimiento de vencidos.
- **Reportes y Estadisticas:** Inasistencias criticas, morosidad, atrasos de inventario y metricas globales institucionales.

---

## 3. Autores
- **Desarrollador / Estudiante:** Equipo de Desarrollo Campuslands / Little Horizons
- **Institucion:** Campuslands & Colegio Internacional Little Horizons S.A.S.
- **Correo Electronico:** contacto@campuslands.com / soporte@littlehorizons.edu.co

---

## 4. Tecnologias Utilizadas
- **Lenguaje de Programacion:** Java 21 (LTS)
- **Herramienta de Construccion:** Apache Maven 3.9+
- **IDE Utilizado:** Apache NetBeans IDE 3.1
- **Base de Datos:** MySQL Server 8.0 o superior
- **Librerias Externas:**
  - `mysql:mysql-connector-j:9.7.0` (Conector JDBC oficial para MySQL)
  - `com.github.librepdf:openpdf:1.3.40` (Generacion de documentos PDF para Facturacion Electronica DIAN)

---

## 5. Requerimientos Previos
- **Java Development Kit (JDK):** Version 21 o superior instalada y configurada en la variable `JAVA_HOME`.
- **Apache Maven:** Version 3.8 o superior (o el Maven embebido en NetBeans).
- **Motor de Base de Datos:** **MySQL Server 8.0 o superior (REQUERIDO)** debido al uso de restricciones `CHECK` nativas (`CHECK (valor_nota >= 0.0 AND valor_nota <= 5.0)`).
- **IDE:** Apache NetBeans IDE (o IntelliJ IDEA / VS Code con extensiones Java).

---

## 6. Configuracion de la Base de Datos

### 6.1. Creacion de la base de datos
Ejecutar el script completo de creacion ubicado en `docs/database.sql` en su cliente MySQL preferido (DBeaver, MySQL Workbench o consola):
```bash
mysql -u root -p < docs/database.sql
```
El script creara la base de datos `erp_little_horizons` con las 22 tablas normalizadas y cargara 30 registros de prueba coherentes por tabla principal.

### 6.2. Configuracion de credenciales
Las credenciales de acceso a la base de datos estan desacopladas del codigo fuente y no se incluyen en el control de versiones Git por seguridad.

1. Localizar la plantilla en: `src/main/resources/db.properties.example`
2. Crear una copia con el nombre: `src/main/resources/db.properties`
3. Configurar los datos de conexion locales:
```properties
db.url=jdbc:mysql://localhost:3306/erp_little_horizons?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.user=erp_user
db.password=campus2023
```

### 6.3. Instalacion del driver JDBC
El controlador `mysql-connector-j` se descarga e instala automaticamente mediante Maven al compilar el proyecto.

---

## 7. Instalacion y Ejecucion

### 7.1. Clonar el repositorio
```bash
git clone https://github.com/usuario/little-horizons-erp.git
cd little-horizons-erp
```

### 7.2. Abrir el proyecto en NetBeans
1. Abrir Apache NetBeans IDE.
2. Ir a **File -> Open Project...**
3. Seleccionar la carpeta del proyecto `PryLittleHorizonsERP` (identificada con el icono de Maven).
4. Esperar a que NetBeans resuelva y descargue las dependencias de Maven.

### 7.3. Compilar
En la terminal o mediante NetBeans (**Clean and Build** / `Shift + F11`):
```bash
mvn clean compile
```

### 7.4. Ejecutar
Mediante NetBeans (**Run Project** / `F6`), o por terminal:
```bash
mvn exec:java
```

---

## 8. Estructura del Proyecto

El software implementa estrictamente una arquitectura en capas basada en **Modelo-Vista-Controlador (MVC)**, complementada con el patron **DAO (Data Access Object)** y una capa de **Servicios (Service Layer)** para la logica de negocio:

```
src/main/java/com/littlehorizons/erp/
├── Main.java               # Punto de entrada de la aplicacion e inyeccion de dependencias
├── model/                  # Entidades del dominio y records inmutables
│   ├── Student.java, Teacher.java, Guardian.java, Subject.java, ...
│   ├── Payment.java, PaymentConcept.java, Resource.java, Loan.java, ...
│   └── BorrowerType.java (Enum de prestatarios: Estudiante / Profesor)
├── dao/                    # Capa de persistencia JDBC con PreparedStatement y try-with-resources
│   ├── StudentDao.java, TeacherDao.java, AcademicRecordDao.java, ...
│   ├── PaymentDao.java, PaymentConceptDao.java, LoanDao.java, ...
│   └── AttendanceDao.java, EventDao.java, ClubDao.java, ...
├── service/                # Capa de reglas de negocio, validaciones y calculos
│   ├── StudentService.java, AcademicRecordService.java, AttendanceService.java, ...
│   ├── PaymentService.java, InvoicePdfService.java, LoanService.java, ...
│   ├── FieldValidator.java (Validaciones reutilizables)
│   └── ValidationException.java (Excepcion controlada para mensajes al usuario)
├── controller/             # Controladores que coordinan el flujo entre la Vista y los Servicios
│   ├── MainController.java, StudentController.java, AcademicController.java, ...
│   ├── FinancialController.java, ResourceController.java, ReportController.java, ...
├── view/                   # Interfaces de consola con validacion inmediata de entradas
│   ├── MainView.java, StudentView.java, FinancialView.java, ResourceView.java, ...
│   └── ReportView.java, AcademicView.java, ClubView.java, ...
└── util/                   # Utilidades transversales
    ├── DatabaseConnection.java (Fabrica de conexiones JDBC desde db.properties)
    ├── ErrorLogger.java (Registro de excepciones en error_log.txt)
    ├── ConsoleInput.java (Lector robusto de teclado con cancelacion)
    └── ConsoleFormat.java (Formateador de tablas y texto alineado)
```

---

## 9. Principios y Patrones de Diseno Aplicados

### 9.1. Principios SOLID
- **S - Single Responsibility Principle (SRP):** Cada clase tiene una unica responsabilidad bien delimitada. Por ejemplo, `StudentDao` se encarga unicamente de persistir y leer estudiantes en MySQL; `StudentService` aplica las reglas de negocio (edades validas, no duplicidad de documento); y `StudentView` gestiona la captura y presentacion en consola.
- **O - Open/Closed Principle (OCP):** Las entidades como `BorrowerType` permiten extender nuevos tipos de prestatarios o metodos de pago sin modificar las logicas centrales de préstamo o cobro.
- **L - Liskov Substitution Principle (LSP):** Uso de tipos polimorficos e interfaces funcionales (`Operation`) en los controladores para estandarizar la ejecucion de transacciones y captura de errores sin alterar el comportamiento de las capas inferiores.
- **I - Interface Segregation Principle (ISP):** Se definen metodos de acceso a datos especificos por entidad en lugar de DAOs genericos sobrecargados de metodos innecesarios.
- **D - Dependency Inversion Principle (DIP):** Los controladores reciben los servicios a traves de sus constructores (Inyeccion de Dependencias en `Main.java`), desacoplando la instanciacion directa y facilitando pruebas unitarias.

### 9.2. Patrones de Diseno
1. **DAO (Data Access Object):** Aislacion total de sentencias SQL y mapeo relacional respecto a la logica de negocio.
2. **Service Layer (Capa de Servicios):** Centraliza las reglas de negocio, validaciones complejas y coordinacion de multiples DAOs dentro de transacciones.
3. **Command Pattern (Funcional):** Implementado en los controladores mediante la interfaz funcional `Operation` y expresiones lambda (`execute(this::registerStudent)`), centralizando el manejo de `ValidationException`, `SQLException` y cancelaciones en un solo punto.
4. **Gestion de Conexiones por Operacion:** En lugar de mantener una conexion estatica abierta que pueda quedar inestable o generar bloqueos concurrentes, `DatabaseConnection.getConnection()` entrega conexiones administradas de forma deterministica mediante bloques `try-with-resources`.

---

## 10. Funcionalidades Principales por Modulo

1. **Modulo 1: Gestion de Estudiantes y Acudientes:**
   - Registro con validacion de documentos colombianos (TI, CC, CE, Pasaporte).
   - Vinculacion muchos a muchos entre estudiantes y acudientes (con roles de contacto de emergencia y responsable financiero).
   - Consultas por documento, actualizacion, retiro logico y eliminacion.
2. **Modulo 2: Gestion Academica:**
   - Matricula por ano lectivo con control de progresion de grados (no saltos ni retrocesos).
   - Asignacion de carga academica y horarios por profesor y seccion.
   - Inscripcion a materias por periodo academico.
   - Registro de calificaciones (0.0 a 5.0) y calculo automatico de promedios por periodo y acumulados mediante Stream API.
   - Control diario de asistencia (Presente, Ausente, Tardanza, Excusa Medica).
3. **Modulo 3: Actividades Extracurriculares:**
   - Catalogo de clubes con validacion en tiempo real de aforo maximo.
   - Inscripcion y retiro de miembros con fecha de vinculacion.
   - Calendario de eventos institucionales y registro de competidores con premiacion.
4. **Modulo 4: Gestion Financiera y Facturacion:**
   - Catalogo de conceptos de cobro (recurrentes y unicos).
   - Registro detallado de pagos con medios de pago formales y recibo consecutivo.
   - Consultas de extractos por estudiante y por acudiente.
   - Reporte de estudiantes con morosidad en mensualidades.
   - **Generacion de Factura Electronica DIAN en PDF:** Genera archivo con resolucion de facturacion, calculo de hash CUFE SHA-256 y codigo QR simulado, guardado en `facturas/`.
5. **Modulo 5: Gestion de Recursos (Biblioteca y Laboratorios):**
   - Inventario con codigo unico, categoria y ubicacion fisica.
   - Prestamos a estudiantes (maximo 3) y profesores (maximo 5) con plazo maximo de 30 dias.
   - Bloqueo automatico de nuevos prestamos si el usuario tiene recursos vencidos.
   - Control de devoluciones con calculo de dias de retraso.
6. **Modulo 6: Reportes y Estadisticas:**
   - Deteccion de ausentismo escolar critico (>3 ausencias por semana).
   - Morosidad general institucional.
   - Prestamos vencidos ordenados por dias de mora.
   - Resumen institucional con conteos globales y total de recaudo.

---

## 11. Manejo de Errores y Logs
- **Estrategia de Excepciones:**
  - `ValidationException`: Excepcion de aplicacion controlada para comunicar mensajes claros en espanol al usuario cuando una regla de negocio se incumple.
  - `ConsoleInput.CancelledException`: Permite al usuario escribir `cancelar` en cualquier campo de texto para abortar la operacion sin alterar datos.
  - `SQLException`: Capturada en la capa de controladores y registrada formalmente.
- **Sistema de Logging:**
  - Las excepciones tecnicas y fallas de base de datos son registradas automaticamente a traves de [`ErrorLogger`](file:///C:/Users/usuario/Documents/NetBeansProjects/PryLittleHorizonsERP/src/main/java/com/littlehorizons/erp/util/ErrorLogger.java).
  - Ubicacion del archivo: `error_log.txt` en la raiz del proyecto.
  - Cada entrada contiene fecha, hora exacta, contexto del error y la traza de ejecucion (`stack trace`).

---

## 12. Capturas de Pantalla
Las evidencias de ejecucion por consola en Apache NetBeans IDE se encuentran documentadas en la carpeta de entrega de la plataforma.

---

## 13. Problemas Conocidos y Limitaciones
- La aplicacion esta disenada para consola de texto interactiva (CLI), por lo que requiere una terminal con soporte para codificacion UTF-8 (configurado de manera automatica en `Main.java`).
- La generacion del codigo QR en la factura PDF incluye el bloque textual formal de verificacion y enlace directo al catalogo de consulta de la DIAN.

---

## 14. Futuras Mejoras
- Migracion a interfaz grafica de usuario moderna (JavaFX o cliente web en Spring Boot / React).
- Integracion con pasarelas de pago reales (PSE, Wompi, PayU).
- Integracion de comunicacion mediante correo electronico automatico (JavaMail) para envio directo de la factura PDF al acudiente.

---

## 15. Licencia
Proyecto educativo y academico desarrollado para evaluacion del ciclo formativo Campuslands 2024. Prohibida su distribucion comercial no autorizada.

---

## 16. Contacto
- **Institucion:** Colegio Internacional Little Horizons S.A.S.
- **Soporte Tecnico:** `soporte@littlehorizons.edu.co`
- **Ubicacion:** Carrera 45 # 128 - 20, Bogota D.C., Colombia

