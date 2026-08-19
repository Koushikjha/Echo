import { useState, useEffect } from "react";
import axios from "axios";
import Login from "./Login";
import Home from "./Home";
import CompleteProfile from './CompleteProfile';

function App() {
  const [page, setPage] = useState(null);

  useEffect(() => {
    const savedPage = localStorage.getItem('page');
    if (savedPage) {
      setPage(savedPage);
    } else {
      setPage('login');
    }
  }, []);

  // persist page changes to localStorage
  const handleSetPage = (newPage) => {
    if (newPage === 'login') {
      localStorage.removeItem('page');
    } else {
      localStorage.setItem('page', newPage);
    }
    setPage(newPage);
  };

  if (page === null) return null;

  return (
      <div>
        {page === 'login'            && <Login           setPage={handleSetPage} />}
        {page === 'complete-profile' && <CompleteProfile setPage={handleSetPage} />}
        {page === 'home'             && <Home            setPage={handleSetPage} />}
      </div>
  );
}

export default App;