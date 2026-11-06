const themes = {

    dark: {

        mode: "dark",

        brand: {
            name: "TicketYa",
            logo: "",
            favicon: "/favicon.ico"
        },

        colors: {

                    primary: "#E91E8C",
                    secondary: "#A78BFA",
                    accent: "#F472B6",

                    background: "#050505",
                    surface: "#151515",
                    surfaceAlt: "#1F1F1F",
                    surfaceHover: "rgba(255,255,255,.10)",

                    text: "#FFFFFF",
                    textSecondary: "#C4C4C4",
                    textMuted: "#7D7D7D",

                    border: "rgba(255,255,255,.12)",

                    success: "#22C55E",
                    warning: "#F59E0B",
                    error: "#EF4444",
                    info: "#3B82F6",

                    headerBg: "#0A0A0A",
                    headerText: "#F5F5F5",
                    headerBorder: "rgba(255,255,255,.1)"

                },

        fonts: {

            title: "Oxanium",
            body: "Inter",
            mono: "Share Tech Mono"

        },

        typography: {

            baseSize: "16px",

            h1: "4rem",
            h2: "3rem",
            h3: "2.25rem",
            h4: "1.75rem",
            h5: "1.25rem",
            h6: "1rem",

            text: {
                xs: "0.75rem",
                sm: "0.875rem",
                base: "1rem",
                md: "1.125rem",
                lg: "1.25rem"
            },

            lineHeight: {

                tight: 1.1,
                normal: 1.5,
                relaxed: 1.7

            },

            weight: {

                light: 300,
                regular: 400,
                medium: 500,
                semibold: 600,
                bold: 700

            }

        },

        spacing: {

            xs: ".25rem",
            sm: ".5rem",
            md: "1rem",
            lg: "2rem",
            xl: "4rem",
            xxl: "6rem"

        },

        radius: {

            xs: "4px",
            sm: "8px",
            md: "12px",
            lg: "20px",
            xl: "32px",
            full: "999px"

        },

        shadows: {

            sm: "0 2px 8px rgba(0,0,0,.15)",
            md: "0 8px 20px rgba(0,0,0,.25)",
            lg: "0 20px 50px rgba(0,0,0,.35)"

        },

        glass: {

            enabled: false,
            background: "transparent",
            border: "transparent",
            blur: "0px"

        },

        buttons: {

            radius: "12px",
            paddingX: "1.5rem",
            paddingY: ".8rem",
            uppercase: false

        },

        cards: {

            radius: "12px",
            padding: "1rem"

        },

        container: {

            default: "1200px",
            wide: "1400px",
            narrow: "900px"

        },

        navbar: {

            height: "88px",
            blur: false,
            transparent: false

        },

        footer: {

            padding: "4rem"

        },

        animation: {

            fast: "150ms",
            normal: "250ms",
            slow: "450ms"

        },

        zIndex: {

            negative: "-1",
            normal: "1",
            hero: "10",
            header: "1000"

        }

    },

    light: {

        mode: "light",

        brand: {
            name: "TicketYa",
            logo: "",
            favicon: "/favicon.ico"
        },

        colors: {

                    primary: "#E91E8C",
                    secondary: "#7C3AED",
                    accent: "#F472B6",

                    background: "#FFFFFF",
                    surface: "#F7F7FA",
                    surfaceAlt: "#EEEEF2",
                    surfaceHover: "rgba(233, 30, 140, 0.08)",

                    text: "#1A1A1A",
                    textSecondary: "#4B4B4B",
                    textMuted: "#7B7B7B",

                    border: "rgba(0,0,0,.10)",

                    success: "#16A34A",
                    warning: "#D97706",
                    error: "#DC2626",
                    info: "#2563EB",

                    /* chrome UI (header / search) */
                    headerBg: "#121212",
                    headerText: "#F5F5F5",
                    headerBorder: "rgba(255,255,255,.08)"

                },

        fonts: {

            title: "Oxanium",
            body: "Inter",
            mono: "Share Tech Mono"

        },

        typography: {

            baseSize: "16px",

            h1: "4rem",
            h2: "3rem",
            h3: "2.25rem",
            h4: "1.75rem",
            h5: "1.25rem",
            h6: "1rem",

            text: {
                xs: "0.75rem",
                sm: "0.875rem",
                base: "1rem",
                md: "1.125rem",
                lg: "1.25rem"
            },

            lineHeight: {

                tight: 1.1,
                normal: 1.5,
                relaxed: 1.7

            },

            weight: {

                light: 300,
                regular: 400,
                medium: 500,
                semibold: 600,
                bold: 700

            }

        },

        spacing: {

            xs: ".25rem",
            sm: ".5rem",
            md: "1rem",
            lg: "2rem",
            xl: "4rem",
            xxl: "6rem"

        },

        radius: {

            xs: "4px",
            sm: "8px",
            md: "12px",
            lg: "20px",
            xl: "32px",
            full: "999px"

        },

        shadows: {

            sm: "0 2px 8px rgba(0,0,0,.08)",
            md: "0 8px 20px rgba(0,0,0,.12)",
            lg: "0 20px 50px rgba(0,0,0,.18)"

        },

        glass: {

            enabled: false,
            background: "transparent",
            border: "transparent",
            blur: "0px"

        },

        buttons: {

            radius: "12px",
            paddingX: "1.5rem",
            paddingY: ".8rem",
            uppercase: false

        },

        cards: {

            radius: "12px",
            padding: "1rem"

        },

        container: {

            default: "1200px",
            wide: "1400px",
            narrow: "900px"

        },

        navbar: {

            height: "88px",
            blur: false,
            transparent: false

        },

        footer: {

            padding: "4rem"

        },

        animation: {

            fast: "150ms",
            normal: "250ms",
            slow: "450ms"

        },

        zIndex: {

            negative: "-1",
            normal: "1",
            hero: "10",
            header: "1000"

        }

    }

};

export const defaultTheme = "light";

export default themes;
