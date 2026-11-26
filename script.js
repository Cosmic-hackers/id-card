"use strict";

const form = document.getElementById("id-form");
const fields = {
  fullName: document.getElementById("full-name"),
  idNumber: document.getElementById("id-number"),
  organization: document.getElementById("organization"),
  designation: document.getElementById("designation"),
  email: document.getElementById("email"),
  phone: document.getElementById("phone"),
  validity: document.getElementById("validity")
};
const photoInput = document.getElementById("photo");
const card = document.getElementById("id-card");
const photoPreview = document.getElementById("preview-photo");
const photoPlaceholder = document.getElementById("photo-placeholder");
const message = document.getElementById("form-message");
const templateSelect = document.getElementById("template-select");
const accentInput = document.getElementById("accent-color");
const colorValue = document.getElementById("color-value");
const photoHint = document.getElementById("photo-hint");
let photoUrl = "";

const templateNames = {
  minimal: "Minimal White",
  gradient: "Modern Gradient",
  black: "Premium Black & Gold",
  glass: "Glassmorphism"
};
const defaults = {
  fullName: "Your Name",
  idNumber: "IDS-0000-00",
  organization: "YOUR ORGANIZATION",
  designation: "Your Designation",
  email: "hello@company.com",
  phone: "+1 555 000 0000",
  validity: "DD / MM / YYYY"
};

function setAccent(value) {
  const [red, green, blue] = value.match(/[a-f\d]{2}/gi).map((part) => parseInt(part, 16));
  card.style.setProperty("--accent", value);
  card.style.setProperty("--accent-rgb", `${red},${green},${blue}`);
  colorValue.textContent = value.toUpperCase();
}

function setText(id, value, fallback) {
  document.getElementById(id).textContent = value.trim() || fallback;
}

function updatePreview() {
  setText("preview-name", fields.fullName.value, defaults.fullName);
  setText("preview-id", fields.idNumber.value, defaults.idNumber);
  setText("preview-organization", fields.organization.value.toUpperCase(), defaults.organization);
  setText("preview-designation", fields.designation.value, defaults.designation);
  setText("preview-email", fields.email.value, defaults.email);
  setText("preview-phone", fields.phone.value, defaults.phone);
  const date = fields.validity.value ? new Date(`${fields.validity.value}T00:00:00`) : null;
  setText("preview-validity", date && !Number.isNaN(date.getTime())
    ? new Intl.DateTimeFormat("en", { day: "2-digit", month: "2-digit", year: "numeric" }).format(date).replaceAll("/", " / ")
    : "", defaults.validity);
}

function applyTemplate(template) {
  card.className = `id-card template-${template}`;
  templateSelect.value = template;
  document.querySelectorAll("[data-template-choice]").forEach((tile) => {
    const selected = tile.dataset.templateChoice === template;
    tile.classList.toggle("is-selected", selected);
    tile.setAttribute("aria-pressed", String(selected));
  });
}

function clearMessage() {
  message.textContent = "";
  message.classList.remove("success");
}

Object.values(fields).forEach((field) => {
  field.addEventListener("input", () => {
    updatePreview();
    field.removeAttribute("aria-invalid");
    clearMessage();
  });
});

templateSelect.addEventListener("change", () => applyTemplate(templateSelect.value));
document.querySelectorAll("[data-template-choice]").forEach((tile) => {
  tile.addEventListener("click", () => applyTemplate(tile.dataset.templateChoice));
});

accentInput.addEventListener("input", () => {
  setAccent(accentInput.value);
});

photoInput.addEventListener("change", () => {
  clearMessage();
  const file = photoInput.files && photoInput.files[0];
  if (!file) {
    photoUrl = "";
    photoPreview.removeAttribute("src");
    photoPreview.hidden = true;
    photoPlaceholder.hidden = false;
    photoHint.textContent = "A clear, front-facing photo works best.";
    return;
  }
  if (!file.type.startsWith("image/")) {
    photoInput.value = "";
    photoUrl = "";
    photoPreview.removeAttribute("src");
    photoPreview.hidden = true;
    photoPlaceholder.hidden = false;
    photoHint.textContent = "Choose a valid image file.";
    message.textContent = "Please choose a valid image file.";
    return;
  }
  if (file.size > 5 * 1024 * 1024) {
    photoInput.value = "";
    photoUrl = "";
    photoPreview.removeAttribute("src");
    photoPreview.hidden = true;
    photoPlaceholder.hidden = false;
    photoHint.textContent = "That file is over 5 MB. Choose a smaller image.";
    message.textContent = "Your photo must be 5 MB or smaller.";
    return;
  }
  const reader = new FileReader();
  reader.addEventListener("load", () => {
    if (typeof reader.result !== "string") {
      message.textContent = "We couldn't read that photo. Please try another image.";
      return;
    }
    photoUrl = reader.result;
    photoPreview.src = photoUrl;
    photoPreview.hidden = false;
    photoPlaceholder.hidden = true;
    photoHint.textContent = file.name;
  });
  reader.addEventListener("error", () => {
    message.textContent = "We couldn't read that photo. Please try another image.";
  });
  reader.readAsDataURL(file);
});

function validateForm() {
  let firstInvalid = null;
  for (const field of Object.values(fields)) {
    const valid = field.value.trim() !== "" && field.checkValidity();
    field.setAttribute("aria-invalid", String(!valid));
    if (!valid && !firstInvalid) firstInvalid = field;
  }
  const photoValid = Boolean(photoUrl && photoInput.files && photoInput.files[0]);
  photoInput.setAttribute("aria-invalid", String(!photoValid));
  if (!photoValid && !firstInvalid) firstInvalid = photoInput;
  if (firstInvalid) {
    message.classList.remove("success");
    message.textContent = firstInvalid === photoInput
      ? "Please add a profile photo before downloading."
      : `Please check the ${firstInvalid.labels[0].textContent.replace("*", "").trim().toLowerCase()} field.`;
    firstInvalid.focus();
    return false;
  }
  return true;
}

document.getElementById("download-button").addEventListener("click", async () => {
  clearMessage();
  if (!validateForm()) return;
  if (typeof window.html2canvas !== "function") {
    message.textContent = "PNG export couldn't load. Check your internet connection and try again.";
    return;
  }
  const button = document.getElementById("download-button");
  button.disabled = true;
  button.textContent = "Preparing your card…";
  try {
    const canvas = await window.html2canvas(card, {
      scale: 3,
      backgroundColor: null,
      useCORS: true,
      logging: false
    });
    const link = document.createElement("a");
    const safeName = fields.fullName.value.trim().replace(/[^\w-]+/g, "-").replace(/^-|-$/g, "").toLowerCase() || "id-card";
    link.download = `${safeName}-idsnap.png`;
    link.href = canvas.toDataURL("image/png");
    link.click();
    message.classList.add("success");
    message.textContent = "Your ID card is ready — the PNG has been downloaded.";
  } catch (error) {
    console.error("Could not export ID card:", error);
    message.textContent = "We couldn't export the card. Please try again.";
  } finally {
    button.disabled = false;
    button.innerHTML = '<span aria-hidden="true">↓</span> Download ID card';
  }
});

document.getElementById("clear-button").addEventListener("click", () => {
  const selectedTemplate = templateSelect.value;
  const selectedAccent = accentInput.value;
  form.reset();
  accentInput.value = selectedAccent;
  setAccent(selectedAccent);
  applyTemplate(selectedTemplate);
  photoUrl = "";
  photoPreview.removeAttribute("src");
  photoPreview.hidden = true;
  photoPlaceholder.hidden = false;
  photoHint.textContent = "A clear, front-facing photo works best.";
  Object.values(fields).forEach((field) => field.removeAttribute("aria-invalid"));
  photoInput.removeAttribute("aria-invalid");
  updatePreview();
  clearMessage();
});

document.getElementById("reset-button").addEventListener("click", () => {
  document.getElementById("clear-button").click();
  accentInput.value = "#7865f5";
  setAccent(accentInput.value);
  applyTemplate("minimal");
});

document.getElementById("year").textContent = String(new Date().getFullYear());
setAccent(accentInput.value);
applyTemplate("minimal");
updatePreview();
