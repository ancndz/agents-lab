package ru.autkaev.agents.techretail.retailer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.autkaev.agents.techretail.smartphone.Smartphone;
import ru.autkaev.agents.techretail.smartphone.SmartphoneOs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Интерфейс добавления товаров продавцу.
 *
 * @author Anton Utkaev
 * @since 2022.06.12
 */
public class TechRetailerAgentGui extends JFrame {

    private static final Logger LOG = LoggerFactory.getLogger(TechRetailerAgentGui.class);

    private JTextField nameField;

    private JTextField ramField;

    private JTextField cpuField;

    private JTextField priceField;

    private JComboBox<SmartphoneOs> osJComboBox;

    TechRetailerAgentGui(final TechRetailerAgent techRetailerAgent) {
        super(techRetailerAgent.getLocalName());
        // Printout a welcome message
        LOG.info("Open seller {} frame.", techRetailerAgent.getName());

        final JPanel panel = addLabels();
        getContentPane().add(panel, BorderLayout.CENTER);

        JButton addButton = new JButton("Add");

        addButton.addActionListener(action -> {
            try {
                techRetailerAgent.addSmartphoneList(new Smartphone().setNonNullName(nameField.getText().trim())
                        .setInstalledRam(Integer.parseInt(ramField.getText().trim()))
                        .setCpuSpeed(Double.parseDouble(cpuField.getText().trim()))
                        .setSmartphoneOs((SmartphoneOs) osJComboBox.getSelectedItem())
                        .setNonNullPrice(Double.parseDouble(priceField.getText().trim())));
                nameField.setText("");
                ramField.setText("");
                cpuField.setText("");
                priceField.setText("");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(TechRetailerAgentGui.this,
                        "Invalid values. " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        setResizable(false);

        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(final WindowEvent e) {
                techRetailerAgent.doDelete();
            }
        });
    }

    /**
     * Добавление полей для ввода.
     *
     * @return панель с полями
     */
    private JPanel addLabels() {
        JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(5, 2));

        panel.add(new JLabel("Model name:"));
        nameField = new JTextField(15);
        panel.add(nameField);

        panel.add(new JLabel("Installed RAM, Mb:"));
        ramField = new JTextField(15);
        panel.add(ramField);

        panel.add(new JLabel("CPU speed, hz:"));
        cpuField = new JTextField(15);
        panel.add(cpuField);

        panel.add(new JLabel("OS:"));
        osJComboBox = new JComboBox<>(SmartphoneOs.values());
        panel.add(osJComboBox);

        panel.add(new JLabel("Price:"));
        priceField = new JTextField(15);
        panel.add(priceField);

        return panel;
    }

    public void showGui() {
        pack();
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int) screenSize.getWidth() / 2;
        int centerY = (int) screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.setVisible(true);
    }

}
