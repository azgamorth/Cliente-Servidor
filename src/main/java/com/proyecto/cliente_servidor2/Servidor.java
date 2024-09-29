package com.proyecto.cliente_servidor2;

import java.io.*;
import java.net.*;
import java.util.*;

public class Servidor {
    // Mapa para mantener los clientes y sus conexiones
    private static Map<String, PrintWriter> clientes = new HashMap<>();

    public static void main(String[] args) {
        try {
            // Crea un servidor socket que escucha en el puerto 8080
            ServerSocket servidorSocket = new ServerSocket(8080);
            System.out.println("Servidor iniciado. Esperando clientes...");

            // Bucle infinito para aceptar múltiples conexiones de clientes
            while (true) {
                // Acepta la conexión de un cliente
                Socket socket = servidorSocket.accept();
                System.out.println("Cliente conectado.");

                // Crea un nuevo hilo para manejar la comunicación con el cliente
                new ClienteHandler(socket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Clase interna que maneja la comunicación con el cliente
    private static class ClienteHandler extends Thread {
        private Socket socket;
        private PrintWriter salidaCliente;
        private String nombreCliente;

        public ClienteHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Prepara el input y output para comunicación
                BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                salidaCliente = new PrintWriter(socket.getOutputStream(), true);

                // Leer el nombre del cliente
                nombreCliente = entrada.readLine();
                System.out.println("Cliente registrado con el nombre: " + nombreCliente);

                // Añadir el cliente a la lista de clientes conectados
                synchronized (clientes) {
                    clientes.put(nombreCliente, salidaCliente);
                    actualizarClientesConectados();
                }

                // Leer y procesar mensajes del cliente
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    if (mensaje.startsWith("Privado:")) {
                        enviarMensajePrivado(mensaje);
                    } else {
                        System.out.println("Mensaje recibido: " + mensaje);
                    }
                }

                // Cuando el cliente se desconecta, lo eliminamos de la lista
                synchronized (clientes) {
                    clientes.remove(nombreCliente);
                    actualizarClientesConectados();
                }

                entrada.close();
                salidaCliente.close();
                socket.close();
                System.out.println("Cliente desconectado.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Método para enviar un mensaje privado a otro cliente
        private void enviarMensajePrivado(String mensaje) {
            String[] partes = mensaje.split(":");
            String destinatario = partes[1];
            String textoMensaje = partes[2];

            // Obtener el PrintWriter del cliente destinatario
            PrintWriter salidaDestinatario;
            synchronized (clientes) {
                salidaDestinatario = clientes.get(destinatario);
            }

            if (salidaDestinatario != null) {
                salidaDestinatario.println("Privado:" + nombreCliente + ":" + textoMensaje);
                salidaCliente.println("Privado:" + nombreCliente + ":" + textoMensaje);  // También envía al remitente
            }
        }

        // Método para actualizar la lista de clientes conectados
        private void actualizarClientesConectados() {
            StringBuilder listaClientes = new StringBuilder("Clientes conectados:");
            synchronized (clientes) {
                for (String cliente : clientes.keySet()) {
                    listaClientes.append(cliente).append(", ");
                }
            }

            // Enviar la lista de clientes conectados a todos
            synchronized (clientes) {
                for (PrintWriter cliente : clientes.values()) {
                    cliente.println(listaClientes.toString());
                }
            }
        }
    }
}
