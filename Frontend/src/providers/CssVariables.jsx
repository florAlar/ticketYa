import { useEffect } from "react";

function walk(obj, prefix = "") {

    const variables = [];

    Object.entries(obj).forEach(([key, value]) => {

        const name = prefix
            ? `${prefix}-${key}`
            : key;

        if (
            typeof value === "object" &&
            value !== null &&
            !Array.isArray(value)
        ) {

            variables.push(...walk(value, name));

        } else {

            variables.push({
                name,
                value
            });

        }

    });

    return variables;

}

export default function CssVariables({ theme }) {

    useEffect(() => {

        const root = document.documentElement;

        walk(theme).forEach(variable => {

            root.style.setProperty(
                `--${variable.name}`,
                variable.value
            );

        });

    }, [theme]);

    return null;

}