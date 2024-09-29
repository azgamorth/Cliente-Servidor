// Nombre del Programa: Aplicación Cliente-Servidor con Chats Privados
// Creado por: Daniel Vargas
// Descripción: Este programa implementa una arquitectura cliente-servidor en la que los clientes pueden conectarse al servidor, ver una lista de otros clientes conectados, 
// y abrir chats privados con ellos. Cada cliente puede enviar y recibir mensajes privados. El servidor central gestiona las conexiones y los mensajes, asegurando que solo
// el destinatario reciba los mensajes privados.

// Ejemplo de lo que hace el código: Un cliente se conecta al servidor, visualiza una lista de clientes conectados y selecciona uno para iniciar una conversación privada. 
// Los mensajes que se envían y reciben entre los clientes se muestran en una ventana de chat privado.

// Librerías usadas:
// - javax.swing: Para crear la interfaz gráfica (ventanas, botones, campos de texto).
// - java.net: Para manejar las conexiones de red mediante sockets.
// - java.io: Para enviar y recibir mensajes a través de las conexiones.

// Funciones principales del código:
// 1. mostrarVentanaPrincipal(): Muestra la ventana principal donde el usuario puede ver la lista de clientes conectados y seleccionar uno para iniciar un chat privado.
// 2. abrirChatConClienteSeleccionado(): Abre una ventana de chat privado con el cliente seleccionado de la lista.
// 3. conectarAlServidor(): Establece la conexión con el servidor, envía el nombre del cliente y escucha los mensajes del servidor.
// 4. actualizarListaClientesConectados(): Actualiza la lista de clientes conectados cada vez que el servidor envía la información.
// 5. actualizarChatPrivado(): Muestra los mensajes recibidos en la ventana de chat privado.

package com.proyecto.cliente_servidor2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class Cliente {
    private JFrame frame; // Ventana principal del cliente
    private JList<String> listaClientesConectados; // Lista para mostrar los clientes conectados
    private DefaultListModel<String> listModel; // Modelo para la lista de clientes conectados
    private JTextField textFieldMensaje; // Campo para escribir mensajes
    private JTextArea textAreaChat; // Área para mostrar el chat principal
    private JTextArea textAreaChatPrivado; // Área para mostrar el chat privado

    private Socket socket; // Socket para conectarse al servidor
    private BufferedReader entrada; // Para leer los mensajes entrantes
    private PrintWriter salida; // Para enviar mensajes
    private String nombreCliente; // Nombre del cliente
    private String clienteSeleccionado; // Cliente seleccionado para chat privado

    // Función principal para iniciar el programa
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Cliente window = new Cliente();
                window.mostrarVentanaPrincipal();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Constructor del cliente
    public Cliente() {
        initialize();
    }

    // Función que muestra la ventana principal y pide al usuario su nombre
    public void mostrarVentanaPrincipal() {
        // Solicitar el nombre del cliente antes de abrir el chat
        nombreCliente = JOptionPane.showInputDialog(frame, "Introduce tu nombre:", "Login", JOptionPane.PLAIN_MESSAGE);

        if (nombreCliente == null || nombreCliente.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Debes ingresar un nombre para continuar.");
            return;
        }

        EventQueue.invokeLater(() -> {
            try {
                frame.setVisible(true);
                conectarAlServidor();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Función que inicializa la ventana principal y los componentes gráficos
    private void initialize() {
        // Configuración básica del JFrame (ventana principal)
        frame = new JFrame("Cliente 1 - Lista de Clientes Conectados");
        frame.setBounds(100, 100, 450, 300);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        // Modelo de lista para los clientes conectados
        listModel = new DefaultListModel<>();
        listaClientesConectados = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(listaClientesConectados);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        // Botón para abrir un chat privado
        JButton btnIniciarChat = new JButton("Iniciar Chat Privado");
        frame.getContentPane().add(btnIniciarChat, BorderLayout.SOUTH);

        // Acción del botón "Iniciar Chat Privado"
        btnIniciarChat.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                abrirChatConClienteSeleccionado(); // Llamar al método para abrir un chat privado
            }
        });
    }

    // Función para abrir un chat privado con el cliente seleccionado
    private void abrirChatConClienteSeleccionado() {
        clienteSeleccionado = listaClientesConectados.getSelectedValue();
        if (clienteSeleccionado != null && !clienteSeleccionado.isEmpty()) {
            // Crear una nueva instancia del chat privado
            JFrame chatFrame = new JFrame("Chat privado con " + clienteSeleccionado);
            chatFrame.setBounds(100, 100, 450, 300);
            chatFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            chatFrame.getContentPane().setLayout(new BorderLayout());

            // Área de texto para mostrar la conversación
            textAreaChatPrivado = new JTextArea();
            textAreaChatPrivado.setEditable(false);
            chatFrame.getContentPane().add(new JScrollPane(textAreaChatPrivado), BorderLayout.CENTER);

            // Panel inferior con un campo de texto para ingresar el mensaje
            JPanel panel = new JPanel();
            chatFrame.getContentPane().add(panel, BorderLayout.SOUTH);
            panel.setLayout(new BorderLayout());

            JTextField textFieldMensajePrivado = new JTextField();
            panel.add(textFieldMensajePrivado, BorderLayout.CENTER);
            textFieldMensajePrivado.setColumns(30);

            // Botón para enviar el mensaje
            JButton btnEnviarPrivado = new JButton("Enviar");
            panel.add(btnEnviarPrivado, BorderLayout.EAST);

            // Acción del botón Enviar para chat privado
            btnEnviarPrivado.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String mensajePrivado = textFieldMensajePrivado.getText();
                    if (!mensajePrivado.isEmpty()) {
                        // Añadir el mensaje al chat privado localmente
                        textAreaChatPrivado.append(nombreCliente + " (tú): " + mensajePrivado + "\n");
                        
                        // Enviar el mensaje al servidor para el cliente seleccionado
                        salida.println("Privado:" + clienteSeleccionado + ":" + mensajePrivado);
                        
                        textFieldMensajePrivado.setText("");
                    }
                }
            });

            chatFrame.setVisible(true);
        } else {
            JOptionPane.showMessageDialog(frame, "Selecciona un cliente de la lista para iniciar un chat.");
        }
    }

    // Función para conectar al servidor y recibir la lista de clientes conectados
    private void conectarAlServidor() {
        try {
            // Conectarse al servidor en localhost y puerto 8080
            socket = new Socket("localhost", 8080);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            // Enviar el nombre del cliente al servidor
            salida.println(nombreCliente);

            // Crear un nuevo hilo para escuchar los mensajes del servidor, incluyendo la lista de clientes conectados
            new Thread(new EscucharClientesConectados()).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error: No se pudo conectar al servidor.");
            e.printStackTrace();
        }
    }

    // Hilo para escuchar la lista de clientes conectados desde el servidor
    private class EscucharClientesConectados implements Runnable {
        public void run() {
            try {
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    if (mensaje.startsWith("Clientes conectados:")) {
                        actualizarListaClientesConectados(mensaje);
                    } else if (mensaje.startsWith("Privado:")) {
                        actualizarChatPrivado(mensaje);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Función para actualizar la lista de clientes conectados
    private void actualizarListaClientesConectados(String mensaje) {
        // Limpiar el modelo de la lista
        listModel.clear();

        // Dividir el mensaje para obtener los clientes conectados (después de "Clientes conectados:")
        String[] partes = mensaje.split(":")[1].split(", ");
        for (String cliente : partes) {
            listModel.addElement(cliente);  // Añadir cada cliente individualmente
        }
    }

    // Función para actualizar el historial del chat privado
    private void actualizarChatPrivado(String mensaje) {
        // Separar la información del mensaje (Privado:clienteDestino:mensaje)
        String[] partes = mensaje.split(":");
        String remitente = partes[1];
        String textoMensaje = partes[2];

        // Mostrar el mensaje en la ventana del chat
        if (textAreaChatPrivado != null) {
            textAreaChatPrivado.append(remitente + ": " + textoMensaje + "\n");
        }
    }
}
