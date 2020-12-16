import axios from 'axios';

/**
 * This is the axios instance configuration (HTTP CLIENT)
 * Configured to use environment varialbes at runtime, provided by helm configuration
 */
export const axiosInstance = axios.create({
    baseURL: `http://${(window as any).REACT_APP_APIHOST!.trim()}`,
    timeout: parseInt((window as any).REACT_APP_HTTP_REQUEST_TIMEOUT!.trim()),
});
