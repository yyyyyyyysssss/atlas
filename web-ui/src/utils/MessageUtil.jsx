import { message } from "antd";

let messageApi = message

export const setMessageApi = (api) => {
    messageApi = api;
}

export const getMessageApi = () => messageApi