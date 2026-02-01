import sys
import numpy as np

def calculate_water_quality(turbidity, ph, chlorine):
    turbidity = float(turbidity)  # 转换为浮动数值
    ph = float(ph)  # 转换为浮动数值
    chlorine = float(chlorine)  # 转换为浮动数值
    # 定义理想值和最大偏差
    ph_ideal = 7.0
    chlorine_ideal = 0.3
    ph_deviation = 1.5
    chlorine_deviation = 0.7

    # 标准化各个指标
    T = 1 - turbidity  # 浊度
    P = 1 - abs(ph - ph_ideal) / ph_deviation  # pH值
    C = 1 - abs(chlorine - chlorine_ideal) / chlorine_deviation  # 余氯

    # 计算每个标准化数据的总和
    total = T + P + C
    pT = T / total
    pP = P / total
    pC = C / total

    # 计算每个指标的信息熵
    p = np.array([pT, pP, pC])
    entropy = -np.sum(p * np.log(p) / np.log(3))

    # 计算差异系数
    diff = 1 - (p * np.log(p) / np.log(3)) / entropy  # 每个指标的差异系数

    # 计算熵权
    total_diff = np.sum(diff)  # diff 的总和
    weights = diff / total_diff  # 每个指标的权重

    # 计算WQI (综合水质指数)
    WQI = weights[0] * T + weights[1] * P + weights[2] * C
    sys.stdout.flush()  # 确保输出被刷新
    return WQI  # 返回 WQI 以便在 main 中打印

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python water_quality.py <turbidity> <ph> <chlorine>")
        sys.exit(1)
    
    turbidity = sys.argv[1]
    ph = sys.argv[2]
    chlorine = sys.argv[3]
    
    result = calculate_water_quality(turbidity, ph, chlorine)
    print(result)
