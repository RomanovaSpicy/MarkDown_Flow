# MarkDown Flow

<p align="center">
  <img src="https://i.postimg.cc/XqsFsq0L/In-Shot-20251105-204111021.jpg" alt="MarkDown Binder Demo" width="300"/>
</p>

A native Android application created to optimize working with Markdown-style text. It uses a system of "binds" (templates) with the ability to intelligently position the cursor using the `|` character.

*Inspired by the [SpicyChat.ai](https://spicychat.ai/) service and developed with support from "The Library of Snailexandria" community.*

---

## 🎨 Key Features

-   **Preset & Custom Binds**: Comes with a default set inspired by SpicyChat markup and allows creating unlimited custom templates.
-   **Smart Cursor Positioning**: Use the `|` symbol in your template to define where the cursor should be placed after insertion.
-   **Floating Overlay**: A quick-access window that snaps to the screen edge for a seamless workflow in any application.
-   **Drag & Drop**: Easily reorder your binds in the main list.
-   **Neomorphic & Glassmorphism UI**: A modern, sleek interface with a dark theme.
-   **Multi-language Support**: Fully translated into 11 languages.
-   **Privacy-Focused**: Works 100% offline. No data collection, no ads.

---

### Required Permissions

The app requires two special permissions for full functionality:

1.  **Display Over Other Apps**: To show the floating overlay window.
2.  **Accessibility Service**: To find input fields, perform text insertion, and position the cursor automatically.

The app will guide you through enabling these permissions on the first launch.

---

## ✅ Project Status: v1.0.0 Beta

All core features are implemented and functional.

#### Roadmap (v1.1 and beyond)

-   [ ] **Data Management**: Implement import/export for user binds.
-   [ ] **Light Theme**: Design and implement a light color scheme.
-   [ ] **Statistics Widgets**: Add more detailed statistics and home screen widgets.
-   [ ] **Data Migration**: Ensure user data is safely preserved across app updates.

---

## 💾 Importing Custom Binds

The application allows you to import your own sets of binds from a `.json` file. This is useful for backing up your data or sharing bind sets with others.

The file must be a JSON array where each object is a bind with three required fields:

-   `"name"`: The name of the bind (e.g., `"Bold Text"`).
-   `"content"`: The template itself. Use `|` to mark the cursor position (e.g., `"**|**"`).
-   `"order"`: The position in the list, starting from `0`.

#### Example `my_binds.json` file:

```json
[
  {
    "name": "Italic",
    "content": "*|*",
    "order": 0
  },
  {
    "name": "Link",
    "content": "[|](url)",
    "order": 1
  },
  {
    "name": "Quote",
    "content": "> |",
    "order": 2
  }
]
```

> ⚠️ **Important:** Importing a file will **replace all** of your current binds. It is recommended to use the export feature to back up your existing binds first.

---

## 👥 Contributing

Contributions are welcome. Please fork the repository, create a new branch for your feature, and submit a pull request. For major changes, please open an issue first to discuss what you would like to change.

---

## 📄 License

This project is licensed under the **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International License**.

See the [LICENSE](LICENSE) file for the full legal text.
