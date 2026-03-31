


/**
 * 格式化文件大小
 * @param {number} bytes 字节数
 * @param {number} decimalPoint 保留小数位
 */
export const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B';
  if (!bytes) return '-';

  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  
  // 计算阶数
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  // 计算数值
  const num = bytes / Math.pow(k, i);

  // 策略：
  // 1. 如果是字节(B)，直接返回整数
  // 2. 如果数值大于 10 或者是 KB，通常取整即可
  // 3. 其他情况保留 1 位小数（如 1.2 MB 比 1 MB 更准确）
  
  const formattedNum = i <= 1 || num >= 10 
    ? Math.round(num) 
    : num.toFixed(1);

  // 去掉 1.0 这种没必要的小数点
  return parseFloat(formattedNum) + ' ' + sizes[i];
};