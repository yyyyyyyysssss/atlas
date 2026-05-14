import React, { useRef, useState } from 'react';
import { Avatar, Upload, Button, Modal, Flex, Typography, App, Image, theme } from 'antd';
import { Pencil } from 'lucide-react';
import ReactCrop, { centerCrop, makeAspectCrop } from 'react-image-crop';
import { useTranslation } from 'react-i18next';
import { useRequest } from 'ahooks';
import 'react-image-crop/dist/ReactCrop.css';
import { simpleUploadFile } from '../../../services/FileService';
import { changeUserProfile } from '../../../services/UserProfileService';
import { useDispatch } from 'react-redux';
import { updateUserInfoPartial } from '../../../redux/slices/userSlice';

const AvatarUpload = ({ avatar, onAvatarChange }) => {
    const { t } = useTranslation();
    const { message } = App.useApp();
    const { token } = theme.useToken();
    const dispatch = useDispatch();
    const [avatarPreviewVisible, setAvatarPreviewVisible] = useState(false);
    const [avatarCropOpen, setAvatarCropOpen] = useState({ open: false, previewImage: null });
    const [crop, setCrop] = useState();
    const [completedCrop, setCompletedCrop] = useState();
    const imgRef = useRef(null);

    const { runAsync: simpleUploadFileAsync, loading: simpleUploadFileLoading } = useRequest(simpleUploadFile, { manual: true });

    const { runAsync: changeUserProfileAsync, loading: changeUserProfileLoading } = useRequest(changeUserProfile, {
        manual: true
    })

    const handleBeforeUpload = (file) => {
        const reader = new FileReader();
        reader.onloadend = () => setAvatarCropOpen({ open: true, previewImage: reader.result });
        reader.readAsDataURL(file);
        return false;
    };

    const handleAvatarCropClose = () => {
        setAvatarCropOpen({ open: false, previewImage: null });
    };

    const handleCropComplete = (c) => {
        setCompletedCrop(c);
        // 如果这里不需要实时绘图，我们什么也不做，只保存坐标
    };

    const handleSetNewAvatar = async () => {
        const image = imgRef.current;
        if (!image || !completedCrop) return;

        const scaleX = image.naturalWidth / image.width;
        const scaleY = image.naturalHeight / image.height;
        const offscreen = new OffscreenCanvas(completedCrop.width * scaleX, completedCrop.height * scaleY);
        const ctx = offscreen.getContext('2d');
        ctx.drawImage(image, completedCrop.x * scaleX, completedCrop.y * scaleY, completedCrop.width * scaleX, completedCrop.height * scaleY, 0, 0, completedCrop.width * scaleX, completedCrop.height * scaleY);

        const blob = await offscreen.convertToBlob({ type: 'image/png' });
        const formData = new FormData();
        formData.append('file', new File([blob], 'avatar.png', { type: 'image/png' }));

        const accessUrl = await simpleUploadFileAsync(formData);
        await changeUserProfileAsync({
            avatar: accessUrl
        })
        dispatch(updateUserInfoPartial({ avatar: accessUrl }))
        message.success('修改成功');
        handleAvatarCropClose();
        onAvatarChange?.(accessUrl);
    };

    return (
        <div style={{ position: 'relative', display: 'inline-block' }}>
            <Avatar size={120} src={avatar} style={{ cursor: 'pointer', boxShadow: '0 0 0 1px #d9d9d9' }} onClick={() => setAvatarPreviewVisible(true)} />
            <Image preview={{ visible: avatarPreviewVisible, src: avatar, onVisibleChange: setAvatarPreviewVisible }} style={{ display: 'none' }} />
            <div style={{ position: 'absolute', bottom: 0, left: 0 }}>
                <Upload showUploadList={false} accept='image/*' beforeUpload={handleBeforeUpload}>
                    <Button shape="circle" icon={<Pencil size={16} />} />
                </Upload>
            </div>
            <Modal
                open={avatarCropOpen.open}
                onCancel={handleAvatarCropClose}
                onOk={handleSetNewAvatar}
                confirmLoading={simpleUploadFileLoading || changeUserProfileLoading}
                width={400}
                centered
                title={<Typography.Title level={5} style={{ margin: 0 }}>{t('更新头像')}</Typography.Title>}
                destroyOnHidden
            >
                <Flex justify="center" align="center" style={{ padding: '16px 0' }}>
                    <ReactCrop
                        crop={crop}
                        aspect={1}
                        circularCrop
                        onChange={(c) => setCrop(c)}
                        onComplete={handleCropComplete}
                    >
                        <img
                            ref={imgRef}
                            src={avatarCropOpen.previewImage}
                            style={{ maxWidth: '100%' }}
                            onLoad={(e) => {
                                const { width, height } = e.currentTarget;
                                setCrop(centerCrop(makeAspectCrop({ unit: '%', width: 100 }, 1, width, height), width, height));
                            }}
                        />
                    </ReactCrop>
                </Flex>
            </Modal>
        </div>
    );
};

export default AvatarUpload;