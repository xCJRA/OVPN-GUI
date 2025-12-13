package openvpnmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author cjra
 */
public class OpenVPNManager extends JFrame {
    
    private JTextField filePathField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton selectFileButton;
    private JButton connectButton;
    private JButton disconnectButton;
    private JTextArea logArea;
    private JLabel statusLabel;
    
    private Process vpnProcess;
    private File selectedConfigFile;
    private volatile boolean isConnected = false;
    
    // Colores para el estado
    private static final Color COLOR_DISCONNECTED = new Color(220, 53, 69);
    private static final Color COLOR_CONNECTING = new Color(255, 193, 7);
    private static final Color COLOR_CONNECTED = new Color(40, 167, 69);
    
    public OpenVPNManager() {
        initializeUI();
        checkOpenVPNInstallation();
    }
    
    /**
     * Inicializa todos los componentes de la interfaz gráfica
     * Buena práctica: Separar la inicialización de UI en un método dedicado
     */
    private void initializeUI() {
        setTitle("OpenVPN Manager");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Panel principal con padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Panel superior con formulario
        JPanel formPanel = createFormPanel();
        mainPanel.add(formPanel, BorderLayout.NORTH);
        
        // Panel central con log
        JPanel logPanel = createLogPanel();
        mainPanel.add(logPanel, BorderLayout.CENTER);
        
        // Panel inferior con estado
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Manejar el cierre de la ventana correctamente
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                disconnect();
            }
        });
    }
    
    /**
     * Crea el panel del formulario con los campos de entrada
     */
    private JPanel createFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuración de Conexión"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Archivo de configuración
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Archivo .ovpn:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1;
        String path = "/home/cjra/Workplace/VPN/profiles/";
        filePathField = new JTextField(path);
        filePathField.setEditable(true);
        panel.add(filePathField, gbc);
        
        gbc.gridx = 2;
        gbc.weightx = 0;
        selectFileButton = new JButton("Seleccionar");
        selectFileButton.addActionListener(e -> selectConfigFile());
        panel.add(selectFileButton, gbc);
        
        // Usuario
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Usuario:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        usernameField = new JTextField();
        panel.add(usernameField, gbc);
        
        // Contraseña
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(new JLabel("Contraseña:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        passwordField = new JPasswordField();
        panel.add(passwordField, gbc);
        
        // Botones de acción
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        connectButton = new JButton("Conectar");
        connectButton.setBackground(COLOR_CONNECTED);
        connectButton.setForeground(Color.WHITE);
        connectButton.setFocusPainted(false);
        connectButton.addActionListener(e -> connect());
        buttonPanel.add(connectButton);
        
        disconnectButton = new JButton("Desconectar");
        disconnectButton.setBackground(COLOR_DISCONNECTED);
        disconnectButton.setForeground(Color.WHITE);
        disconnectButton.setFocusPainted(false);
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(e -> disconnect());
        buttonPanel.add(disconnectButton);
        
        panel.add(buttonPanel, gbc);
        
        return panel;
    }
    
    /**
     * Crea el panel de log para mostrar mensajes
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Registro de Conexión"));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Crea el panel de estado en la parte inferior
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        statusLabel = new JLabel("● Desconectado");
        statusLabel.setForeground(COLOR_DISCONNECTED);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        panel.add(statusLabel);
        
        return panel;
    }
    
    /**
     * Verifica si OpenVPN está instalado en el sistema
     * Buena práctica: Validar dependencias al inicio
     */
    private void checkOpenVPNInstallation() {
        String os = System.getProperty("os.name").toLowerCase();
        String command = os.contains("win") ? "openvpn --version" : "which openvpn";
        
        try {
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                appendLog("✓ OpenVPN encontrado en el sistema");
            } else {
                appendLog("⚠ OpenVPN no encontrado. Por favor, instálalo primero.");
                JOptionPane.showMessageDialog(this,
                    "OpenVPN no está instalado en tu sistema.\n" +
                    "Por favor, descárgalo de: https://openvpn.net/community-downloads/",
                    "OpenVPN no encontrado",
                    JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            appendLog("⚠ No se pudo verificar la instalación de OpenVPN: " + e.getMessage());
        }
    }
    
    /**
     * Abre un diálogo para seleccionar el archivo .ovpn
     */
    private void selectConfigFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".ovpn");
            }
            
            @Override
            public String getDescription() {
                return "Archivos OpenVPN (*.ovpn)";
            }
        });
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedConfigFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedConfigFile.getAbsolutePath());
            appendLog("Archivo seleccionado: " + selectedConfigFile.getName());
        }
    }
    
    /**
     * Establece la conexión VPN
     * Buena práctica: Ejecutar operaciones largas en un hilo separado
     */
    private void connect() {
        // Validaciones
        if (selectedConfigFile == null || !selectedConfigFile.exists()) {
            JOptionPane.showMessageDialog(this,
                "Por favor, selecciona un archivo de configuración válido.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Por favor, ingresa usuario y contraseña.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Deshabilitar botón de conectar
        connectButton.setEnabled(false);
        disconnectButton.setEnabled(true);
        updateStatus("Conectando...", COLOR_CONNECTING);
        appendLog("\n--- Iniciando conexión VPN ---");
        
        // Ejecutar conexión en hilo separado para no bloquear la UI
        new Thread(() -> {
            try {
                // Crear archivo temporal con credenciales
                File authFile = createAuthFile(username, password);
                
                // Construir comando
                List<String> command = buildOpenVPNCommand(authFile);
                
                appendLog("Ejecutando: " + String.join(" ", command));
                
                // Iniciar proceso
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                vpnProcess = pb.start();
                
                // Leer salida del proceso
                monitorVPNProcess();
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    appendLog("✗ Error al conectar: " + e.getMessage());
                    updateStatus("Error de conexión", COLOR_DISCONNECTED);
                    connectButton.setEnabled(true);
                    JOptionPane.showMessageDialog(this,
                        "Error al conectar: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    /**
     * Crea un archivo temporal con las credenciales
     * Buena práctica: Manejar credenciales de forma segura
     */
    private File createAuthFile(String username, String password) throws IOException {
        File authFile = File.createTempFile("ovpn_auth_", ".txt");
        authFile.deleteOnExit(); // Eliminar al salir del programa
        
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(authFile), StandardCharsets.UTF_8))) {
            writer.write(username);
            writer.newLine();
            writer.write(password);
        }
        
        // Establecer permisos restrictivos (solo lectura para el propietario)
        authFile.setReadable(false, false);
        authFile.setReadable(true, true);
        authFile.setWritable(false, false);
        
        return authFile;
    }
    
    /**
     * Construye el comando para ejecutar OpenVPN
     */
    private List<String> buildOpenVPNCommand(File authFile) {
        List<String> command = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        
        if (os.contains("win")) {
            // Windows
            command.add("openvpn");
        } else {
            // Linux/Mac - requiere sudo
            command.add("sudo");
            command.add("openvpn");
        }
        
        command.add("--config");
        command.add(selectedConfigFile.getAbsolutePath());
        command.add("--auth-user-pass");
        command.add(authFile.getAbsolutePath());
        
        return command;
    }
    
    /**
     * Monitorea la salida del proceso VPN
     * Buena práctica: Proporcionar feedback en tiempo real al usuario
     */
    private void monitorVPNProcess() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(vpnProcess.getInputStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                final String logLine = line;
                
                SwingUtilities.invokeLater(() -> appendLog(logLine));
                
                // Detectar estados de conexión
                if (line.contains("Initialization Sequence Completed")) {
                    SwingUtilities.invokeLater(() -> {
                        isConnected = true;
                        updateStatus("Conectado", COLOR_CONNECTED);
                        connectButton.setEnabled(false);
                        disconnectButton.setEnabled(true);
                        appendLog("\n✓ Conexión establecida exitosamente");
                    });
                } else if (line.contains("AUTH_FAILED")) {
                    SwingUtilities.invokeLater(() -> {
                        appendLog("\n✗ Autenticación fallida. Verifica tus credenciales.");
                        disconnect();
                        JOptionPane.showMessageDialog(this,
                            "Autenticación fallida. Verifica usuario y contraseña.",
                            "Error de autenticación", JOptionPane.ERROR_MESSAGE);
                    });
                } else if (line.contains("Cannot resolve host")) {
                    SwingUtilities.invokeLater(() -> {
                        appendLog("\n✗ No se puede resolver el servidor VPN.");
                    });
                }
            }
            
            // Si el proceso termina
            SwingUtilities.invokeLater(() -> {
                if (isConnected) {
                    appendLog("\n✗ Conexión VPN terminada");
                }
                disconnect();
            });
            
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                appendLog("Error leyendo salida: " + e.getMessage());
            });
        }
    }
    
    /**
     * Desconecta la VPN
     */
    private void disconnect() {
        if (vpnProcess != null && vpnProcess.isAlive()) {
            appendLog("\n--- Cerrando conexión VPN ---");
            vpnProcess.destroy();
            
            // Esperar a que termine
            try {
                vpnProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        isConnected = false;
        updateStatus("Desconectado", COLOR_DISCONNECTED);
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        vpnProcess = null;
    }
    
    /**
     * Actualiza el label de estado
     */
    private void updateStatus(String text, Color color) {
        statusLabel.setText("● " + text);
        statusLabel.setForeground(color);
    }
    
    /**
     * Añade una línea al área de log
     * Buena práctica: Auto-scroll al final
     */
    private void appendLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    /**
     * Método principal
     */
    public static void main(String[] args) {
        // Configurar Look and Feel nativo del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Crear y mostrar la ventana en el Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            OpenVPNManager manager = new OpenVPNManager();
            manager.setVisible(true);
        });
    }
    
}
