import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080", // API Gateway
  withCredentials: true // IMPORTANT for cookies 🍪
});

export default api;