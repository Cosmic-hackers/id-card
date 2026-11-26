package app.idsnap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.geom.RoundRectangle2D;

public class IdSnapApp extends JFrame {
    private static final Color PAGE = new Color(9, 13, 28);
    private static final Color PANEL = new Color(16, 22, 41);
    private static final Color INPUT = new Color(11, 17, 32);
    private static final Color TEXT = new Color(241, 243, 255);
    private static final Color MUTED = new Color(143, 153, 179);
    private static final Color PURPLE = new Color(120, 101, 245);
    private static final String[] TEMPLATES = {
        "Minimal White", "Modern Gradient", "Premium Black & Gold", "Glassmorphism"
    };

    private final JTextField fullName = new JTextField();
    private final JTextField idNumber = new JTextField();
    private final JTextField organization = new JTextField();
    private final JTextField designation = new JTextField();
    private final JTextField email = new JTextField();
    private final JTextField phone = new JTextField();
    private final JTextField validity = new JTextField();
    private final JTextField department = new JTextField();
    private final JTextField bloodGroup = new JTextField();
    private final JTextField emergencyContact = new JTextField();
    private final JTextField issueDate = new JTextField();
    private final JComboBox<String> template = new JComboBox<String>(TEMPLATES);
    private final JLabel photoLabel = new JLabel("No photo selected");
    private final JLabel colorLabel = new JLabel("#7865F5");
    private final JLabel editorHint = new JLabel("Select an element to move it; use the buttons to add or edit elements.");
    private final CardPreview cardPreview = new CardPreview();
    private final Timer qrUpdateTimer = new Timer(250, event -> refreshContactQr());
    private BufferedImage photo;
    private Color accent = PURPLE;
    private boolean accentCustomized;

    public IdSnapApp() {
        super("IDSnap — ID Card Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 620));
        setSize(1180, 790);
        setLocationRelativeTo(null);
        getContentPane().setBackground(PAGE);
        setLayout(new BorderLayout());
        add(createHeader(), BorderLayout.NORTH);
        add(createWorkspace(), BorderLayout.CENTER);
        installLiveUpdates();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PAGE);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(35, 42, 64)),
            BorderFactory.createEmptyBorder(15, 25, 15, 25)
        ));
        JLabel brand = new JLabel("▥  IDSnap");
        brand.setFont(new Font("SansSerif", Font.BOLD, 21));
        brand.setForeground(TEXT);
        JLabel note = new JLabel("PREMIUM ID CARD GENERATOR");
        note.setFont(new Font("SansSerif", Font.BOLD, 10));
        note.setForeground(MUTED);
        header.add(brand, BorderLayout.WEST);
        header.add(note, BorderLayout.EAST);
        return header;
    }

    private JPanel createWorkspace() {
        JPanel workspace = new JPanel(new GridBagLayout());
        workspace.setBackground(PAGE);
        workspace.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JScrollPane formScroll = new JScrollPane(createEditor());
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getViewport().setBackground(PANEL);
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.setPreferredSize(new Dimension(430, 640));

        JPanel previewPanel = createPreviewPanel();
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 0.44;
        left.weighty = 1;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(0, 0, 0, 14);
        workspace.add(formScroll, left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = 0;
        right.weightx = 0.56;
        right.weighty = 1;
        right.fill = GridBagConstraints.BOTH;
        workspace.add(previewPanel, right);
        return workspace;
    }

    private JPanel createEditor() {
        JPanel editor = new JPanel();
        editor.setLayout(new javax.swing.BoxLayout(editor, javax.swing.BoxLayout.Y_AXIS));
        editor.setBackground(PANEL);
        editor.setBorder(BorderFactory.createEmptyBorder(20, 21, 18, 21));

        JLabel heading = new JLabel("01   YOUR DETAILS");
        styleSectionHeading(heading);
        editor.add(heading);
        editor.add(BoxGap.vertical(14));

        addField(editor, "Full name *", fullName);
        addField(editor, "ID number *", idNumber);
        addField(editor, "College / company *", organization);
        addField(editor, "Designation *", designation);
        addField(editor, "Email address *", email);
        addField(editor, "Phone number *", phone);
        addField(editor, "Validity date *  (YYYY-MM-DD)", validity);
        addField(editor, "Department (optional)", department);
        addField(editor, "Blood group (optional)", bloodGroup);
        addField(editor, "Emergency contact (optional)", emergencyContact);
        addField(editor, "Issue date (optional, YYYY-MM-DD)", issueDate);

        JButton photoButton = createSecondaryButton("Choose profile photo…");
        photoButton.addActionListener(this::choosePhoto);
        editor.add(photoButton);
        photoLabel.setForeground(MUTED);
        photoLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        photoLabel.setBorder(BorderFactory.createEmptyBorder(6, 2, 13, 0));
        editor.add(photoLabel);

        editor.add(BoxGap.divider());
        editor.add(BoxGap.vertical(14));
        JLabel designHeading = new JLabel("02   MAKE IT YOURS");
        styleSectionHeading(designHeading);
        editor.add(designHeading);
        editor.add(BoxGap.vertical(12));
        addField(editor, "Card template", template);

        JPanel colorRow = new JPanel(new BorderLayout(8, 0));
        colorRow.setBackground(PANEL);
        JLabel colorTitle = new JLabel("Accent color");
        colorTitle.setForeground(new Color(190, 197, 216));
        colorTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JButton colorButton = createSecondaryButton("Choose…");
        colorButton.addActionListener(event -> chooseAccent());
        colorLabel.setForeground(TEXT);
        colorLabel.setFont(new Font("Monospaced", Font.BOLD, 11));
        JPanel colorInfo = new JPanel(new BorderLayout(8, 0));
        colorInfo.setOpaque(false);
        colorInfo.add(colorTitle, BorderLayout.WEST);
        colorInfo.add(colorLabel, BorderLayout.EAST);
        colorRow.add(colorInfo, BorderLayout.CENTER);
        colorRow.add(colorButton, BorderLayout.EAST);
        colorRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 17, 0));
        editor.add(colorRow);

        JButton download = createPrimaryButton("Download ID card as PNG");
        download.addActionListener(this::downloadCard);
        download.setAlignmentX(LEFT_ALIGNMENT);
        editor.add(download);
        editor.add(BoxGap.vertical(8));

        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        JButton clear = createSecondaryButton("Clear form");
        clear.addActionListener(event -> clearForm());
        JButton reset = createSecondaryButton("Reset design");
        reset.addActionListener(event -> resetForm());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 5);
        actions.add(clear, c);
        c.gridx = 1;
        c.insets = new Insets(0, 5, 0, 0);
        actions.add(reset, c);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        editor.add(actions);
        editor.add(BoxGap.vertical(10));
        JLabel privacy = new JLabel("Your details and photo stay on this computer.");
        privacy.setForeground(MUTED);
        privacy.setFont(new Font("SansSerif", Font.PLAIN, 10));
        privacy.setAlignmentX(LEFT_ALIGNMENT);
        editor.add(privacy);
        return editor;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(36, 44, 67)),
            BorderFactory.createEmptyBorder(18, 18, 17, 18)
        ));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel live = new JLabel("●  LIVE PREVIEW");
        live.setForeground(new Color(135, 222, 181));
        live.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel size = new JLabel("CR-80  ·  85.6 × 54 mm");
        size.setForeground(MUTED);
        size.setFont(new Font("SansSerif", Font.PLAIN, 10));
        top.add(live, BorderLayout.WEST);
        top.add(size, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        JPanel editorControls = new JPanel(new BorderLayout(0, 8));
        editorControls.setOpaque(false);
        JPanel tools = new JPanel(new java.awt.GridLayout(2, 3, 7, 7));
        tools.setOpaque(false);
        JButton addText = createSecondaryButton("+ Text");
        addText.addActionListener(event -> addTextElement());
        JButton addQr = createSecondaryButton("+ Contact QR");
        addQr.addActionListener(event -> addQrElement());
        JButton addShape = createSecondaryButton("+ Accent shape");
        addShape.addActionListener(event -> addShapeElement());
        JButton editText = createSecondaryButton("Edit selected text");
        editText.addActionListener(event -> editSelectedText());
        JButton removeElement = createSecondaryButton("Remove selected");
        removeElement.addActionListener(event -> removeSelectedElement());
        tools.add(addText);
        tools.add(addQr);
        tools.add(addShape);
        tools.add(editText);
        tools.add(removeElement);
        editorControls.add(tools, BorderLayout.NORTH);
        editorHint.setForeground(MUTED);
        editorHint.setFont(new Font("SansSerif", Font.PLAIN, 10));
        editorControls.add(editorHint, BorderLayout.SOUTH);
        cardPreview.setPreferredSize(new Dimension(560, 390));
        JPanel previewBody = new JPanel(new BorderLayout(0, 10));
        previewBody.setOpaque(false);
        previewBody.add(editorControls, BorderLayout.NORTH);
        previewBody.add(cardPreview, BorderLayout.CENTER);
        panel.add(previewBody, BorderLayout.CENTER);

        JLabel privacy = new JLabel("Everything happens locally — no account or upload.");
        privacy.setHorizontalAlignment(JLabel.CENTER);
        privacy.setForeground(MUTED);
        privacy.setFont(new Font("SansSerif", Font.PLAIN, 11));
        panel.add(privacy, BorderLayout.SOUTH);
        return panel;
    }

    private void addField(JPanel parent, String labelText, java.awt.Component field) {
        JPanel group = new JPanel(new BorderLayout(0, 6));
        group.setBackground(PANEL);
        group.setAlignmentX(LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, field instanceof JComboBox ? 67 : 68));
        JLabel label = new JLabel(labelText);
        label.setForeground(new Color(190, 197, 216));
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        group.add(label, BorderLayout.NORTH);

        if (field instanceof JTextField) {
            JTextField input = (JTextField) field;
            input.setBackground(INPUT);
            input.setForeground(TEXT);
            input.setCaretColor(TEXT);
            input.setFont(new Font("SansSerif", Font.PLAIN, 12));
            input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(43, 52, 77)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            input.setPreferredSize(new Dimension(250, 36));
        } else if (field instanceof JComboBox) {
            @SuppressWarnings("unchecked")
            JComboBox<String> choices = (JComboBox<String>) field;
            choices.setBackground(INPUT);
            choices.setForeground(TEXT);
            choices.setFont(new Font("SansSerif", Font.PLAIN, 12));
            choices.setFocusable(false);
            choices.addActionListener(event -> cardPreview.repaint());
            choices.setPreferredSize(new Dimension(250, 36));
        }
        group.add(field, BorderLayout.CENTER);
        parent.add(group);
        parent.add(BoxGap.vertical(7));
    }

    private void styleSectionHeading(JLabel label) {
        label.setForeground(new Color(176, 164, 255));
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setAlignmentX(LEFT_ALIGNMENT);
    }

    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(PURPLE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(28, 36, 58));
        button.setForeground(TEXT);
        button.setFocusPainted(false);
        button.setFont(new Font("SansSerif", Font.PLAIN, 11));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(49, 59, 85)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    private void installLiveUpdates() {
        JTextField[] inputs = { fullName, idNumber, organization, designation, email, phone, validity,
            department, bloodGroup, emergencyContact, issueDate };
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { detailChanged(); }
            public void removeUpdate(DocumentEvent event) { detailChanged(); }
            public void changedUpdate(DocumentEvent event) { detailChanged(); }
        };
        for (JTextField input : inputs) {
            input.getDocument().addDocumentListener(listener);
        }
    }

    private void detailChanged() {
        cardPreview.repaint();
        if (cardPreview.hasQrElement()) {
            qrUpdateTimer.restart();
        }
    }

    private void choosePhoto(ActionEvent event) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a profile photo");
        chooser.setFileFilter(new FileNameExtensionFilter("Image files (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selected = chooser.getSelectedFile();
        if (selected.length() > 5L * 1024L * 1024L) {
            showError("Choose an image that is 5 MB or smaller.");
            return;
        }
        try {
            BufferedImage loaded = ImageIO.read(selected);
            if (loaded == null) {
                showError("That file could not be read as an image. Choose a JPG, PNG, or GIF.");
                return;
            }
            photo = loaded;
            photoLabel.setText(selected.getName());
            cardPreview.repaint();
        } catch (IOException exception) {
            showError("Could not read the selected photo: " + exception.getMessage());
        }
    }

    private void chooseAccent() {
        Color selected = JColorChooser.showDialog(this, "Choose an accent color", accent);
        if (selected != null) {
            accent = selected;
            accentCustomized = true;
            colorLabel.setText(String.format("#%02X%02X%02X", accent.getRed(), accent.getGreen(), accent.getBlue()));
            cardPreview.repaint();
        }
    }

    private void addTextElement() {
        String text = JOptionPane.showInputDialog(this, "Enter text for the card:", "NEW TEXT");
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        cardPreview.addElement(new CardElement(ElementType.TEXT, 500, 530, 360, 42, text.trim()));
        editorHint.setText("Text added. Drag it into place, or edit the selected text.");
    }

    private void addShapeElement() {
        cardPreview.addElement(new CardElement(ElementType.SHAPE, 830, 145, 150, 16, ""));
        editorHint.setText("Accent shape added. Drag it anywhere on the card.");
    }

    private void addQrElement() {
        if (fullName.getText().trim().isEmpty()) {
            showError("Enter a full name before generating the contact QR code.");
            fullName.requestFocusInWindow();
            return;
        }
        CardElement qr = cardPreview.getQrElement();
        if (qr == null) {
            qr = new CardElement(ElementType.QR, 915, 306, 285, 290, "");
            cardPreview.addElement(qr);
        } else {
            cardPreview.selectElement(qr);
        }
        refreshContactQr();
    }

    private void editSelectedText() {
        CardElement selected = cardPreview.getSelectedElement();
        if (selected == null || selected.type != ElementType.TEXT) {
            showError("Select a text element on the card first.");
            return;
        }
        String updated = JOptionPane.showInputDialog(this, "Edit card text:", selected.text);
        if (updated != null && !updated.trim().isEmpty()) {
            selected.text = updated.trim();
            cardPreview.repaint();
        }
    }

    private void removeSelectedElement() {
        if (!cardPreview.removeSelectedElement()) {
            showError("Select an added element on the card first.");
            return;
        }
        editorHint.setText("Element removed. Add another element or select one to move it.");
    }

    private void refreshContactQr() {
        CardElement qr = cardPreview.getQrElement();
        if (qr == null) {
            return;
        }
        if (fullName.getText().trim().isEmpty()) {
            qr.image = null;
            editorHint.setText("Enter a name to generate the contact QR; it refreshes as details change.");
            cardPreview.repaint();
            return;
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, Integer.valueOf(2));
            BitMatrix matrix = new QRCodeWriter().encode(
                buildContactVCard(), BarcodeFormat.QR_CODE, 240, 240, hints
            );
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }
            qr.image = image;
            editorHint.setText("Contact QR ready. Drag it into place; it updates with your contact details.");
        } catch (WriterException exception) {
            qr.image = null;
            editorHint.setText("QR generation failed: " + exception.getMessage());
        }
        cardPreview.repaint();
    }

    private String buildContactVCard() {
        String name = escapeVCard(fullName.getText().trim());
        StringBuilder card = new StringBuilder("BEGIN:VCARD\nVERSION:3.0\n");
        card.append("N:").append(name).append(";;;;\n");
        card.append("FN:").append(name).append('\n');
        String company = organization.getText().trim();
        String team = department.getText().trim();
        if (!company.isEmpty() || !team.isEmpty()) {
            card.append("ORG:").append(escapeVCard(company));
            if (!team.isEmpty()) {
                card.append(';').append(escapeVCard(team));
            }
            card.append('\n');
        }
        appendVCardField(card, "TITLE", designation.getText());
        appendVCardField(card, "EMAIL;TYPE=INTERNET", email.getText());
        appendVCardField(card, "TEL;TYPE=CELL", phone.getText());
        card.append("END:VCARD");
        return card.toString();
    }

    private static void appendVCardField(StringBuilder card, String key, String value) {
        if (!value.trim().isEmpty()) {
            card.append(key).append(':').append(escapeVCard(value.trim())).append('\n');
        }
    }

    private static String escapeVCard(String value) {
        return value.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\n")
            .replace("\n", "\\n");
    }

    private void downloadCard(ActionEvent event) {
        if (!validateForm()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save your ID card");
        chooser.setSelectedFile(new File(safeFileName(fullName.getText()) + "-idsnap.png"));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG image", "png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File destination = chooser.getSelectedFile();
        if (!destination.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
            destination = new File(destination.getParentFile(), destination.getName() + ".png");
        }
        if (destination.exists()) {
            int choice = JOptionPane.showConfirmDialog(this,
                "The file already exists. Replace it?", "Confirm overwrite", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            BufferedImage image = cardPreview.renderImage();
            if (!ImageIO.write(image, "png", destination)) {
                showError("This Java installation could not write PNG images.");
                return;
            }
            JOptionPane.showMessageDialog(this, "ID card saved to:\n" + destination.getAbsolutePath(),
                "Download complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException exception) {
            showError("Could not save the PNG: " + exception.getMessage());
        }
    }

    private boolean validateForm() {
        JTextField[] inputs = { fullName, idNumber, organization, designation, email, phone, validity };
        String[] names = { "Full name", "ID number", "College / company", "Designation", "Email address", "Phone number", "Validity date" };
        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i].getText().trim().isEmpty()) {
                showError(names[i] + " is required.");
                inputs[i].requestFocusInWindow();
                return false;
            }
        }
        if (!email.getText().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            showError("Enter a valid email address.");
            email.requestFocusInWindow();
            return false;
        }
        try {
            LocalDate.parse(validity.getText().trim());
        } catch (DateTimeParseException exception) {
            showError("Enter a valid date in YYYY-MM-DD format.");
            validity.requestFocusInWindow();
            return false;
        }
        if (!issueDate.getText().trim().isEmpty()) {
            try {
                LocalDate.parse(issueDate.getText().trim());
            } catch (DateTimeParseException exception) {
                showError("Enter a valid issue date in YYYY-MM-DD format.");
                issueDate.requestFocusInWindow();
                return false;
            }
        }
        if (photo == null) {
            showError("Choose a profile photo before saving.");
            return false;
        }
        return true;
    }

    private void clearForm() {
        JTextField[] inputs = { fullName, idNumber, organization, designation, email, phone, validity,
            department, bloodGroup, emergencyContact, issueDate };
        for (JTextField input : inputs) {
            input.setText("");
        }
        photo = null;
        photoLabel.setText("No photo selected");
        cardPreview.repaint();
    }

    private void resetForm() {
        clearForm();
        template.setSelectedIndex(0);
        accent = PURPLE;
        accentCustomized = false;
        colorLabel.setText("#7865F5");
        cardPreview.removeAllElements();
        cardPreview.repaint();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Check your details", JOptionPane.WARNING_MESSAGE);
    }

    private static String safeFileName(String input) {
        String safe = input.trim().replaceAll("[^A-Za-z0-9_-]+", "-").replaceAll("^-|-$", "");
        return safe.isEmpty() ? "id-card" : safe.toLowerCase(Locale.ROOT);
    }

    private enum ElementType {
        TEXT, QR, SHAPE
    }

    private static class CardElement {
        final ElementType type;
        int x;
        int y;
        int width;
        int height;
        String text;
        BufferedImage image;

        CardElement(ElementType type, int x, int y, int width, int height, String text) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
        }
    }

    private class CardPreview extends JPanel {
        private static final int CARD_WIDTH = 1280;
        private static final int CARD_HEIGHT = 806;
        private final ArrayList<CardElement> elements = new ArrayList<CardElement>();
        private CardElement selectedElement;
        private int dragOffsetX;
        private int dragOffsetY;

        CardPreview() {
            setOpaque(false);
            setMinimumSize(new Dimension(350, 240));
            MouseAdapter editorMouse = new MouseAdapter() {
                public void mousePressed(MouseEvent event) {
                    int[] point = toCardPoint(event);
                    selectedElement = elementAt(point[0], point[1]);
                    if (selectedElement != null) {
                        dragOffsetX = point[0] - selectedElement.x;
                        dragOffsetY = point[1] - selectedElement.y;
                        editorHint.setText("Selected " + elementName(selectedElement) + ". Drag to move it.");
                    } else {
                        editorHint.setText("Select an added element to move, edit, or remove it.");
                    }
                    repaint();
                }

                public void mouseDragged(MouseEvent event) {
                    if (selectedElement == null) {
                        return;
                    }
                    int[] point = toCardPoint(event);
                    selectedElement.x = Math.max(8, Math.min(CARD_WIDTH - selectedElement.width - 8, point[0] - dragOffsetX));
                    selectedElement.y = Math.max(8, Math.min(CARD_HEIGHT - selectedElement.height - 8, point[1] - dragOffsetY));
                    repaint();
                }
            };
            addMouseListener(editorMouse);
            addMouseMotionListener(editorMouse);
        }

        private int[] toCardPoint(MouseEvent event) {
            double scale = Math.min((getWidth() - 24.0) / CARD_WIDTH, (getHeight() - 24.0) / CARD_HEIGHT);
            if (scale <= 0) {
                return new int[] { 0, 0 };
            }
            int width = (int) (CARD_WIDTH * scale);
            int height = (int) (CARD_HEIGHT * scale);
            int offsetX = (getWidth() - width) / 2;
            int offsetY = (getHeight() - height) / 2;
            return new int[] {
                (int) ((event.getX() - offsetX) / scale),
                (int) ((event.getY() - offsetY) / scale)
            };
        }

        private CardElement elementAt(int x, int y) {
            for (int i = elements.size() - 1; i >= 0; i--) {
                CardElement element = elements.get(i);
                if (new Rectangle(element.x, element.y, element.width, element.height).contains(x, y)) {
                    return element;
                }
            }
            return null;
        }

        private String elementName(CardElement element) {
            if (element.type == ElementType.QR) {
                return "contact QR";
            }
            return element.type == ElementType.TEXT ? "text" : "accent shape";
        }

        void addElement(CardElement element) {
            elements.add(element);
            selectedElement = element;
            repaint();
        }

        CardElement getQrElement() {
            for (CardElement element : elements) {
                if (element.type == ElementType.QR) {
                    return element;
                }
            }
            return null;
        }

        boolean hasQrElement() {
            return getQrElement() != null;
        }

        CardElement getSelectedElement() {
            return selectedElement;
        }

        void selectElement(CardElement element) {
            selectedElement = element;
            repaint();
        }

        boolean removeSelectedElement() {
            if (selectedElement == null) {
                return false;
            }
            elements.remove(selectedElement);
            selectedElement = null;
            repaint();
            return true;
        }

        void removeAllElements() {
            elements.clear();
            selectedElement = null;
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                double scale = Math.min((getWidth() - 24.0) / CARD_WIDTH, (getHeight() - 24.0) / CARD_HEIGHT);
                if (scale <= 0) {
                    return;
                }
                int width = (int) (CARD_WIDTH * scale);
                int height = (int) (CARD_HEIGHT * scale);
                g.translate((getWidth() - width) / 2.0, (getHeight() - height) / 2.0);
                g.scale(scale, scale);
                drawCard(g, true);
            } finally {
                g.dispose();
            }
        }

        BufferedImage renderImage() {
            BufferedImage image = new BufferedImage(CARD_WIDTH, CARD_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                drawCard(g, false);
            } finally {
                g.dispose();
            }
            return image;
        }

        private void drawCard(Graphics2D g, boolean editorOverlay) {
            String selectedTemplate = (String) template.getSelectedItem();
            Color cardAccent = "Premium Black & Gold".equals(selectedTemplate) && !accentCustomized
                ? new Color(214, 182, 105) : accent;
            boolean dark = !"Minimal White".equals(selectedTemplate);
            Color ink = dark ? Color.WHITE : new Color(28, 34, 50);
            Color subdued = dark ? new Color(208, 213, 230) : new Color(112, 121, 143);
            Color backgroundStart;
            Color backgroundEnd;
            if ("Modern Gradient".equals(selectedTemplate)) {
                backgroundStart = cardAccent.darker();
                backgroundEnd = new Color(18, 34, 61);
            } else if ("Premium Black & Gold".equals(selectedTemplate)) {
                backgroundStart = new Color(31, 31, 36);
                backgroundEnd = new Color(9, 10, 14);
            } else if ("Glassmorphism".equals(selectedTemplate)) {
                backgroundStart = new Color(100, 129, 165);
                backgroundEnd = new Color(37, 57, 91);
            } else {
                backgroundStart = new Color(250, 250, 253);
                backgroundEnd = new Color(240, 242, 249);
            }

            g.setPaint(new GradientPaint(0, 0, backgroundStart, CARD_WIDTH, CARD_HEIGHT, backgroundEnd));
            g.fillRoundRect(0, 0, CARD_WIDTH, CARD_HEIGHT, 44, 44);
            g.setColor(new Color(255, 255, 255, dark ? 35 : 180));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(2, 2, CARD_WIDTH - 5, CARD_HEIGHT - 5, 42, 42);
            g.setColor(withAlpha(cardAccent, "Minimal White".equals(selectedTemplate) ? 28 : 50));
            g.fillOval(920, -170, 500, 500);

            g.setColor(cardAccent);
            g.fillRoundRect(55, 48, 50, 50, 15, 15);
            g.setColor(Color.WHITE);
            g.fillRoundRect(70, 63, 5, 18, 3, 3);
            g.fillRoundRect(81, 57, 5, 30, 3, 3);
            g.fillRoundRect(92, 66, 5, 15, 3, 3);
            g.setFont(new Font("SansSerif", Font.BOLD, 25));
            g.setColor(ink);
            g.drawString("IDSNAP", 120, 73);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(subdued);
            g.drawString("IDENTITY CARD", 121, 93);

            g.setColor(withAlpha(cardAccent, dark ? 50 : 30));
            g.fillRoundRect(1050, 56, 165, 37, 18, 18);
            g.setColor(ink);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString("●  ACTIVE", 1082, 80);

            int photoX = 64;
            int photoY = 155;
            int photoWidth = 300;
            int photoHeight = 390;
            g.setColor(withAlpha(cardAccent, 38));
            g.fillRoundRect(photoX - 5, photoY - 5, photoWidth + 10, photoHeight + 10, 28, 28);
            if (photo != null) {
                drawPhoto(g, photo, photoX, photoY, photoWidth, photoHeight);
            } else {
                g.setColor(withAlpha(cardAccent, dark ? 70 : 43));
                g.fillRoundRect(photoX, photoY, photoWidth, photoHeight, 24, 24);
                String initials = initials(fullName.getText());
                g.setFont(new Font("SansSerif", Font.BOLD, 86));
                g.setColor(dark ? Color.WHITE : cardAccent);
                drawCentered(g, initials.isEmpty() ? "ID" : initials, photoX, photoY + 205, photoWidth);
            }
            g.setColor(withAlpha(ink, 85));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(photoX, photoY, photoWidth, photoHeight, 24, 24);

            int infoX = 425;
            int infoWidth = hasQrElement() ? 465 : 785;
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.setColor(cardAccent);
            g.drawString(ellipsize(g, textOr(organization.getText(), "YOUR ORGANIZATION").toUpperCase(Locale.ROOT), infoWidth), infoX, 205);
            g.setFont(new Font("SansSerif", Font.BOLD, 55));
            g.setColor(ink);
            g.drawString(ellipsize(g, textOr(fullName.getText(), "Your Name"), infoWidth), infoX, 285);
            g.setFont(new Font("SansSerif", Font.PLAIN, 27));
            g.setColor(subdued);
            g.drawString(ellipsize(g, textOr(designation.getText(), "Your Designation"), infoWidth), infoX, 333);
            drawOptionalLine(g, "DEPARTMENT", department.getText(), infoX, 370, infoWidth, subdued);

            g.setFont(new Font("SansSerif", Font.PLAIN, 19));
            g.setColor(subdued);
            g.drawString(ellipsize(g, textOr(email.getText(), "hello@company.com"), infoWidth), infoX, 410);
            g.drawString(ellipsize(g, textOr(phone.getText(), "+1 555 000 0000"), infoWidth), infoX, 452);
            drawOptionalLine(g, "EMERGENCY", emergencyContact.getText(), infoX, 492, infoWidth, subdued);
            drawOptionalLine(g, "BLOOD GROUP", bloodGroup.getText(), infoX, 530, infoWidth, subdued);

            int footerY = 610;
            g.setColor(withAlpha(ink, 55));
            g.setStroke(new BasicStroke(2));
            g.drawLine(60, footerY, 1218, footerY);
            drawFooterItem(g, "ID NUMBER", textOr(idNumber.getText(), "IDS-0000-00"), 65, footerY + 34, 275, ink, subdued);
            String issued = formattedDate(issueDate.getText());
            drawFooterItem(g, "ISSUED ON", issued.isEmpty() ? "DD / MM / YYYY" : issued, 390, footerY + 34, 270, ink, subdued);
            String valid = formattedDate(validity.getText());
            drawFooterItem(g, "VALID THROUGH", valid.isEmpty() ? "DD / MM / YYYY" : valid, 715, footerY + 34, 285, ink, subdued);
            drawBarcode(g, 1060, footerY + 9, ink);

            for (CardElement element : elements) {
                drawElement(g, element, cardAccent, ink, subdued);
            }
            if (editorOverlay && selectedElement != null) {
                java.awt.Stroke original = g.getStroke();
                g.setColor(new Color(135, 205, 255));
                g.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] { 9, 6 }, 0));
                g.drawRoundRect(selectedElement.x - 4, selectedElement.y - 4,
                    selectedElement.width + 8, selectedElement.height + 8, 12, 12);
                g.setStroke(original);
            }
        }

        private void drawOptionalLine(Graphics2D g, String title, String value, int x, int y, int width, Color color) {
            if (value.trim().isEmpty()) {
                return;
            }
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.setColor(color);
            String text = title + "  ·  " + value.trim();
            g.drawString(ellipsize(g, text, width), x, y);
        }

        private void drawElement(Graphics2D g, CardElement element, Color cardAccent, Color ink, Color subdued) {
            if (element.type == ElementType.SHAPE) {
                g.setColor(cardAccent);
                g.fillRoundRect(element.x, element.y, element.width, element.height, element.height, element.height);
            } else if (element.type == ElementType.TEXT) {
                g.setColor(withAlpha(cardAccent, 42));
                g.fillRoundRect(element.x, element.y, element.width, element.height, 12, 12);
                g.setColor(ink);
                g.setFont(new Font("SansSerif", Font.BOLD, 20));
                g.drawString(ellipsize(g, element.text, element.width - 18), element.x + 9,
                    element.y + Math.min(element.height - 7, 28));
            } else {
                g.setColor(Color.WHITE);
                g.fillRoundRect(element.x, element.y, element.width, element.height, 9, 9);
                if (element.image != null) {
                    int qrSize = Math.min(element.width - 16, element.height - 30);
                    int qrX = element.x + (element.width - qrSize) / 2;
                    g.drawImage(element.image, qrX, element.y + 7, qrSize, qrSize, null);
                } else {
                    g.setColor(subdued);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    g.drawString("Contact QR", element.x + 10, element.y + element.height / 2);
                }
                g.setColor(new Color(35, 42, 59));
                g.setFont(new Font("SansSerif", Font.BOLD, 9));
                String caption = "SCAN TO SAVE CONTACT";
                FontMetrics metrics = g.getFontMetrics();
                g.drawString(caption, element.x + (element.width - metrics.stringWidth(caption)) / 2,
                    element.y + element.height - 8);
            }
        }

        private void drawPhoto(Graphics2D g, BufferedImage image, int x, int y, int width, int height) {
            Graphics2D photoGraphics = (Graphics2D) g.create();
            try {
                Shape clip = new RoundRectangle2D.Float(x, y, width, height, 24, 24);
                photoGraphics.setClip(clip);
                double scale = Math.max(width / (double) image.getWidth(), height / (double) image.getHeight());
                int drawWidth = (int) Math.ceil(image.getWidth() * scale);
                int drawHeight = (int) Math.ceil(image.getHeight() * scale);
                int drawX = x + (width - drawWidth) / 2;
                int drawY = y + (height - drawHeight) / 2;
                photoGraphics.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
            } finally {
                photoGraphics.dispose();
            }
        }

        private void drawFooterItem(Graphics2D g, String title, String value, int x, int y, int width, Color ink, Color subdued) {
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(subdued);
            g.drawString(title, x, y);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.setColor(ink);
            g.drawString(ellipsize(g, value, width), x, y + 31);
        }

        private void drawBarcode(Graphics2D g, int x, int y, Color ink) {
            g.setColor(withAlpha(ink, 180));
            int[] widths = { 4, 2, 7, 3, 5, 2, 6, 3, 2, 8, 3, 5, 2, 4, 7, 2, 5, 3, 6, 2 };
            int position = x;
            for (int i = 0; i < widths.length; i++) {
                g.fillRect(position, y, widths[i], 47);
                position += widths[i] + 4;
            }
        }

        private void drawCentered(Graphics2D g, String value, int x, int baseline, int width) {
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(value, x + (width - metrics.stringWidth(value)) / 2, baseline);
        }

        private String ellipsize(Graphics2D g, String value, int maxWidth) {
            FontMetrics metrics = g.getFontMetrics();
            if (metrics.stringWidth(value) <= maxWidth) {
                return value;
            }
            String trimmed = value;
            while (trimmed.length() > 1 && metrics.stringWidth(trimmed + "…") > maxWidth) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            return trimmed + "…";
        }
    }

    private static String initials(String value) {
        String[] parts = value.trim().split("\\s+");
        if (value.trim().isEmpty()) {
            return "";
        }
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private static String textOr(String value, String fallback) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static String formattedDate(String value) {
        try {
            return LocalDate.parse(value.trim()).format(DateTimeFormatter.ofPattern("MM / dd / yyyy"));
        } catch (DateTimeParseException exception) {
            return "";
        }
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static class BoxGap {
        static JPanel vertical(int height) {
            JPanel gap = new JPanel();
            gap.setOpaque(false);
            gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            gap.setPreferredSize(new Dimension(1, height));
            return gap;
        }

        static JPanel divider() {
            JPanel divider = new JPanel();
            divider.setBackground(new Color(40, 48, 70));
            divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            divider.setPreferredSize(new Dimension(1, 1));
            return divider;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            System.err.println("Using the default Swing look and feel: " + exception.getMessage());
        }
        SwingUtilities.invokeLater(() -> new IdSnapApp().setVisible(true));
    }
}
