import { Home, User, Settings, LogOut } from "lucide-react";
import { NavLink } from "react-router-dom";

const Sidebar = () => {
  return (
    <div className="w-16 h-screen bg-card fixed left-0 top-0 border-r border-border/50 flex flex-col items-center py-4 gap-4">
      <div className="mb-8">
        <h1 className="text-xl font-bold">N</h1>
      </div>
      
      <NavLink to="/" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
        <Home size={20} />
      </NavLink>
      
      <NavLink to="/profile" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
        <User size={20} />
      </NavLink>
      
      <NavLink to="/settings" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
        <Settings size={20} />
      </NavLink>
      
      <div className="mt-auto">
        <button className="nav-link text-destructive">
          <LogOut size={20} />
        </button>
      </div>
    </div>
  );
};

export default Sidebar;