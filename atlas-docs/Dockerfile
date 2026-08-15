# 基础镜像
FROM crpi-ka9mjef5kpn3p7h6.cn-shanghai.personal.cr.aliyuncs.com/ys0921/nginx:alpine

LABEL maintainer="Yan Shuai"

RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/nginx.conf

COPY ./build /usr/share/nginx/html

RUN chown -R nginx:nginx /usr/share/nginx/html


# 暴露端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]