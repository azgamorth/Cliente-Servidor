package com.proyecto.cliente_servidor2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

public class Cliente {
    private JFrame frame;
    private JList<String> listaClientesConectados;
    private DefaultListModel<String> listModel;
    private JTextField textFieldMensaje;
    private JTextArea textAreaChat;
    private JTextArea textAreaChatPrivado;

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String nombreCliente;
    private String clienteSeleccionado;

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

    public Cliente() {
        initialize();
    }

    // Método para mostrar la ventana principal
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

    // Método para abrir el chat con el cliente seleccionado
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

    // Método para conectar al servidor y recibir la lista de clientes conectados
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

    // Método para actualizar la lista de clientes conectados
    private void actualizarListaClientesConectados(String mensaje) {
        // Limpiar el modelo de la lista
        listModel.clear();

        // Dividir el mensaje para obtener los clientes conectados (después de "Clientes conectados:")
        String[] partes = mensaje.split(":")[1].split(", ");
        for (String cliente : partes) {
            listModel.addElement(cliente);  // Añadir cada cliente individualmente
        }
    }

    // Método para actualizar el historial del chat privado
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
