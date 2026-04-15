import { Flex, Typography } from "antd";
import CardRenderer from "./renderers/CardRenderer";
import FileRenderer from "./renderers/FileRenderer";
import TextRenderer from "./renderers/TextRenderer";
import React from "react";

const { Text } = Typography

const RENDERER_MAP = {
    TEXT: TextRenderer,
    FILE: FileRenderer,
    CARD: CardRenderer,
};

const MessageRenderer = ({ notificationId, content, onClose }) => {

    const { renderType, body } = content

    const Renderer = RENDERER_MAP[renderType]

    if (!Renderer) {
        return <Text type="secondary">未知类型</Text>;
    }

    const handleAction = (notificationId) => {
        console.log('handleAction', notificationId)
    }

    return (
        <Flex
            vertical
            style={{
                width: '100%',
                cursor: 'pointer'
            }}
            onClick={(e) => {
                handleAction(notificationId)
            }}
        >
            <Renderer
                content={body}
                onClose={onClose}
            />
        </Flex>
    )
}

export default React.memo(MessageRenderer)