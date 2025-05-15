# DATOS IMPORTANTES PARA CORRER EL PROYECTO - LEER PRIMERO

## PASOS PARA EJECUTAR
1. Navegar a la carpeta del backend: `cd ruta/a/MtdrSpring/backend`
2. Ejecutar estos comandos en orden:
```
mvn clean install
mvn spring-boot:run
```

## CONFIGURACIÓN PARA EJECUCIÓN LOCAL
Para ejecutar el proyecto en entorno local, es necesario:

1. Crear un archivo `.env` en la raíz del proyecto con las credenciales necesarias
2. Modificar el path de la wallet en `application.properties` para que coincida con tu ubicación local

### Configuración del archivo application.properties
Ajusta la siguiente línea en el archivo application.properties:

spring.datasource.url=jdbc:oracle:thin:@localdb_high?TNS_ADMIN=<TU_RUTA_A_LA_WALLET>

Ejemplo:
```
spring.datasource.url=jdbc:oracle:thin:@localdb_high?TNS_ADMIN=C:/Users/josem/OneDrive/Escritorio/OracleSprint2/Wallet_LocalDB
```

## CREDENCIALES Y DATOS DE CONFIGURACIÓN

### Oracle Cloud
- OCID Compartment: `ocid1.compartment.oc1..aaaaaaaatv5o4fgmxcdcn3t7d7ukizkxoht6awq5qtboddkzeee56nnqnswq`
- MTDR_DB_OCID: `ocid1.autonomousdatabase.oc1.mx-queretaro-1.anyxeljrafjygrqar3nghwauftvpd2w7yvmtltnpzpftv6bp6ozqmn2e3m5q`
- Database PASSWORD: `oracleTeam14`

### Credenciales UI
- User ID: `equipo14Oracle`
- Password: `toomanyPSWDS`

### Telegram Bot
- Bot Username: `team14oracle_bot`
- API Token: `7594571838:AAHWJa8AmrmJafNUF2VSWXaMLY-pLP-d-S0`

### Local Database
- LocalDataBase = `LocalDataBase1`
- Wallet Local = `LocalWallet1`
- ToDoUserTable = `Oracle123456!`

---
