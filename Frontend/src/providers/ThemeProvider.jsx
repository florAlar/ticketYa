import { useEffect, useMemo, useState } from "react";
import ThemeContext from "../context/ThemeContext";
import CssVariables from "./CssVariables";

import themes, { defaultTheme } from "../config/theme";

const THEME_KEY = "ticketya_theme";
const LEGACY_THEME_KEY = "theme";

function getInitialTheme() {
    // Clave nueva: no hereda el "dark" de la plantilla Zero Studio
    const saved = localStorage.getItem(THEME_KEY);
    if (saved && themes[saved]) {
        return saved;
    }

    // Descartar legacy
    localStorage.removeItem(LEGACY_THEME_KEY);

    return defaultTheme; // light
}

export default function ThemeProvider({ children }) {
    const [themeName, setThemeName] = useState(getInitialTheme);

    const theme = useMemo(() => themes[themeName] || themes[defaultTheme], [themeName]);

    useEffect(() => {
        localStorage.setItem(THEME_KEY, themeName);
        localStorage.removeItem(LEGACY_THEME_KEY);
        document.documentElement.setAttribute("data-theme", themeName);
        document.documentElement.style.colorScheme = theme?.mode || themeName;
    }, [themeName, theme]);

    const value = {
        theme,
        themeName,
        setTheme: setThemeName,
        themes,
    };

    return (
        <ThemeContext.Provider value={value}>
            <CssVariables theme={theme} />
            {children}
        </ThemeContext.Provider>
    );
}
