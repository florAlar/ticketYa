// hooks/useSite.js
import { useContext } from "react";
import SiteContext from "../context/SiteContext";

export default function useSite() {
  const context = useContext(SiteContext);
  
  if (!context) {
    throw new Error("useSite debe ser utilizado dentro de un SiteProvider");
  }
  
  return context;
}