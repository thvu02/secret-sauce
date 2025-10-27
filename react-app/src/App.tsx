import { BrowserRouter, Route, Routes } from 'react-router-dom';
import Home from './frontend/pages/Home.tsx';
import './App.css';

const App = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Home />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;