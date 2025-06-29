# Backend de Questi.on

Este repositorio contiene el código fuente del *backend* de la aplicación web `Questi.on`, un sistema de gamificación educativa que genera preguntas dinámicamente utilizando Inteligencia Artificial. El *backend* está diseñado con una arquitectura de microservicios que gestiona la comunicación síncrona y asíncrona, la lógica de negocio, la interacción con la API de OpenAI y la persistencia de datos.

## 🔵 Tecnologías Utilizadas

El *backend* ha sido desarrollado utilizando las siguientes tecnologías:

* **Java / Spring Boot:** Framework principal para el desarrollo de microservicios.
* **Spring Cloud Feign Client:** Para facilitar la comunicación síncrona entre microservicios (HTTP/REST).
* **Apache Kafka:** Plataforma de *streaming* de eventos para la comunicación asíncrona entre microservicios.
* **MongoDB:** Base de datos NoSQL para la persistencia de datos de usuarios y partidas.
* **Docker / Docker Compose:** Para la orquestación y el despliegue local de la infraestructura de contenedores.
* **OpenAI API:** API externa utilizada para la generación dinámica de preguntas y respuestas.

## 🟣 Herramientas Necesarias

Antes de empezar, se deben instalar los siguientes programas en tu entorno local:

* **Java Development Kit (JDK):** Necesario para compilar y ejecutar las aplicaciones Spring Boot.
* **Maven:** Herramienta de construcción del proyecto.
* **Docker Desktop (o Docker Engine y Docker Compose):** Imprescindible para ejecutar la infraestructura de bases de datos y Kafka en contenedores.
* **Clave API de OpenAI:** Necesitarás una clave para configurar el microservicio de IA, que podrás conseguir registrándote en la web oficial de la API de OpenAI.

## 🟢 Puesta en Marcha del Proyecto

Sigue estos pasos para arrancar el proyecto de *backend* en tu máquina local:

1.  **Clonar el Repositorio:**
    ```bash
    git clone [https://github.com/cmaganareguero/questi.on-back](https://github.com/cmaganareguero/questi.on-back)
    cd questi-on-backend
    ```

2.  **Arrancar la Infraestructura con Docker Compose:**
    Este paso desplegará los contenedores de MongoDB y la infraestructura de comunicación asíncrona de Apache Kafka.
    ```bash
    docker-compose up -d
    ```
    Asegúrate de que los servicios se inician correctamente. Puedes verificarlo con `docker-compose ps`.

3.  **Compilar y Arrancar los Microservicios Spring Boot:**
    Antes de arrancar los microservicios, asegúrate de compilar el proyecto para resolver todas las dependencias:
    ```bash
    mvn clean install
    ```
    Una vez compilado, puedes arrancar los microservicios de dos maneras:

    * **Desde la línea de comandos:**
      Navega al directorio de cada microservicio (por ejemplo, `./authorization-service`) y ejecuta individualmente:
        ```bash
        mvn spring-boot:run
        ```

    * **Desde IntelliJ IDEA (usando el archivo `.run.xml`):**
      Si utilizas IntelliJ IDEA, puedes usar el archivo de configuración de ejecución `Arrancar microservicios.run.xml`. Este archivo está diseñado para arrancar todos los microservicios a la vez con la configuración adecuada dentro del IDE, facilitando la depuración y gestión centralizada.