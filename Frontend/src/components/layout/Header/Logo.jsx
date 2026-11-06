// src/components/Logo.jsx
import { Link } from 'react-router-dom';
import { FaStar } from 'react-icons/fa';
import useSite from '../../../hooks/useSite';

const Logo = ({ onClick }) => {
  const { siteInfo } = useSite();

  const brand = siteInfo?.brand;
  const brandName = brand?.name || 'TicketYa';

  return (
    <Link to="/" className="logo-container" onClick={onClick}>
      <FaStar className="logo-icon" aria-hidden />
      <span className="logo-text">{brandName}</span>
    </Link>
  );
};

export default Logo;
