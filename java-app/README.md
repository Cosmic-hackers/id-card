# IDSnap Java Desktop App

This is the standalone Java Swing version of IDSnap. The contact QR is generated locally with ZXing; form data and photos are not sent to a service.

## Requirements

- JDK 8 or newer
- Maven 3.6.3 or newer
- Internet access for the first Maven build to download ZXing and Maven plugins

## Run on Windows

Double-click `run.bat`, or open a terminal in this folder and run:

```sh
mvn package
java -jar target/idsnap-app-1.0.0.jar
```

## Editor and card details

- Add, drag, edit, and remove text, accent shapes, and a contact QR from the live card preview.
- The QR contains a vCard (name, organization, department, designation, email, and phone). It is regenerated locally when those details change.
- Optional fields include department, blood group, emergency contact, and issue date.
- The QR and added elements are included in PNG exports. PNG export remains available with no QR if you do not add one.
- Photos are processed locally and limited to 5 MB.
