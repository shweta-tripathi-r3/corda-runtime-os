import axios from 'axios';

export type FlowTypes =
    | 'net.corda.demo.connectfour.StartConnectFourGameFlow'
    | 'net.corda.demo.tictactoe.StartTicTacToeGameFlow';
const authConfig = {
    username: 'admin',
    password: 'admin',
};
const axiosInstance = axios.create({
    //baseURL: 'http://localhost:3000',
    auth: authConfig,
});

export default axiosInstance;
