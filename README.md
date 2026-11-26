# IDSnap — Premium ID Card Generator

IDSnap includes a responsive browser-based ID card designer built with HTML, CSS, and vanilla JavaScript, and a standalone Java Swing desktop app. Both process form details and photos locally; there is no backend or account.

## Java desktop app

The Java Swing version is in [`java-app`](./java-app/). It includes a draggable card editor, local vCard QR generation, additional card details, and PNG export. On Windows, double-click `java-app/run.bat` with a JDK and Maven installed. See [`java-app/README.md`](./java-app/README.md) for requirements and run instructions.

## Run locally

1. Download or clone this project.
2. Open `index.html` in a modern browser.
3. Fill in the required fields, upload a JPG or PNG photo (up to 5 MB), and choose a template and accent color.
4. Select **Download ID card** to save a high-resolution PNG.

For the most consistent local-file behavior, you can optionally serve the folder with any static HTTP server. The page loads Google Fonts and html2canvas from their public CDNs, so internet access is required for those assets and PNG export.

## Features

- Four templates: Minimal White, Modern Gradient, Premium Black & Gold, and Glassmorphism.
- Live card preview and accent color customization.
- Required-field and email validation before export.
- Photo preview, file type and size checks, and helpful validation messages.
- Clear form preserves the design; reset also restores the default template and color.
- Responsive layouts for phones, tablets, and desktop screens.
- PNG export powered by html2canvas.

## Deploy

This is a static site. Publish `index.html`, `style.css`, and `script.js` at the root of a GitHub Pages or Cloudflare Pages project. No build step or server-side configuration is needed.
