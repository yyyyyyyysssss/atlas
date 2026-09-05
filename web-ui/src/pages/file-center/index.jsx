import React, { useState } from 'react';
import './index.css';
import { Flex } from 'antd';
import SmartUpload from '../../components/smart-upload';

const FileCenter = () => {

    return (
        <Flex>
            <SmartUpload
                listType='text'
                multiple
                onSuccess={(file) => {
                    console.log('上传成功', file);
                }}
            />
        </Flex>
    )
}

export default FileCenter;