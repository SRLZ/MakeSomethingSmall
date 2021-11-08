#! /bin/bash
#下载k8s的镜像源
#因为下面的字符串中包含_ 所以使用的时候用${imageRepositoryForK8s}
imageRepositoryForK8s="registry\.aliyuncs.com\/google\_containers"
#服务器的ip 一般为服务器内网ip
advertiseAddress="172.22.30.121"
podSubnet="10.244.0.0/16"
serviceSubnet="10.96.0.0/12"
K8sVersion="1.21.0"
##############version management#################
kubelet="1.21.1-00" 
kubernetescni="0.8.7-00"
kubeadm="1.21.1-00"
kubectl="1.21.1-00"
################################################
if [[ "$#" -lt 1 ]];then
  echo "must with at least one parameter:
  --help usage for help
  -p the dirrectory where admin.conf locates
     default /root/itcast
  -k install the k8s
  -k -m  install the k8s(master)
  -c install the openCAS"
  exit 1
fi

if [[ $1 == "--help" ]];then
  echo "it is the utils for install opencase/k8s"
  echo "install the openCAS with ./utils -c"
  echo "install the k8s(#the admin.config is in the /root/itcast by default#)"
  echo "install the k8s(master) ./utils -k -m "
  echo "- with ./utils -k"
  echo "install k8s having your dir with ./utils -k -p your-dir"
  echo "you can install together with ./utils -k -p your-dir"
  exit 0
fi

isk8s="0"
isCAS="0"
isMaster="0"
dir="/root/itcast"
for (( index=0; index <= $#; index++ ))
do
  if [[ ${!index} == "-c" ]];then
    isCAS="1"
  elif [[ ${!index} == "-k" ]];then
    isk8s="1"
  elif [[ ${!index} == "-p" ]];then
    i=$((index+1))
    dir=${!i}
  elif [[ ${!index} == "-m" ]];then
    isMaster="1"
  fi
done
echo "+++++(0 不安装 1 安装)+++++++"
echo "dir: $dir"
echo "isCAS: $isCAS"
echo "isk8s: $isk8s"
echo "isMaster: $isMaster"
echo "+++++++++++++++++++++++++++++"
#exit 0
if [ $isk8s == "1" ];then
echo "deb http://mirrors.ustc.edu.cn/kubernetes/apt kubernetes-xenial main" >> /etc/apt/sources.list.d/kubernetes.list
gpg --keyserver keyserver.ubuntu.com --recv-keys 307EA071
gpg --export --armor 307EA071 | sudo apt-key add -
sudo apt update
apt update
sudo ufw disable
sudo swapoff -a
apt-get install selinux-utils
apt-get install docker-io
setenforce 0
sudo getenforce
echo "net.bridge.bridge-nf-call-ip6tables = 1" > /etc/sysctl.d/k8s.conf
echo "net.bridge.bridge-nf-call-iptables = 1" >> /etc/sysctl.d/k8s.conf
echo "vm.swappiness = 0" >> /etc/sysctl.d/k8s.conf
sudo modprobe br_netfilter
sudo sysctl -p /etc/sysctl.d/k8s.conf
apt-get install -y kubelet=$kubelet kubernetes-cni=$kubernetescni kubeadm=$kubeadm kubectl=$kubectl
systemctl enable kubelet && systemctl start kubelet
kubectl version
if [[ $isMaster == "1" ]] ;then
  kubeadm config print init-defaults > ./kubeadm.conf
  echo $advertiseAddress | xargs -I {} sed -i 's/advertiseAddress: .*/advertiseAddress: {}/g' ./kubeadm.conf
  echo "$advertiseAddress"
  echo $K8sVersion | xargs -I {} sed -i 's/kubernetesVersion: .*/kubernetesVersion: {}/g' ./kubeadm.conf
  #当替换的字符串中包含/时 ，需要将分隔符替换为其他类型（比如#）紧跟s的会被认为是分隔符
  echo ${imageRepositoryForK8s} | xargs -I {} sed -i 's%imageRepository: .*%imageRepository: {}%g' ./kubeadm.conf
  #foundIndex=`grep -n "podSubnet:" kubeadm.conf | head -1 | cut -d ":" -f 1`
  echo $podSubnet | xargs -I {} sed -i '/serviceSubnet:.*/i \  podSubnet: {}' kubeadm.conf
  kubeadm config images list --config kubeadm.conf
  dir="/etc/kubernetes"
  kubeadm config images pull --config ./kubeadm.conf
  sudo kubeadm init --config ./kubeadm.conf
  echo -e "\e[1;33mthe image of coredns may be not found
  you can check it by docker images|grep coredns
  you can be pull by command below:
    docker pull registry.aliyuncs.com/google_containers/coredns:1.8.0
      docker tag registry.aliyuncs.com/google_containers/coredns:1.8.0 registry.aliyuncs.com/google_containers/coredns:v1.8.0\e[0m"
  echo "-- flannel config --"
  if [ ! -e kube-flannel.yml ];then
     echo "not existed ! download from remote......"
     wget https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
  fi
  kubectl apply -f kube-flannel.yml
else
  echo "-- you can join the master cluster now --"
fi
  mkdir -p $HOME/.kube
  cp -i $dir/admin.conf /root/.kube
  chown $(id -u):$(id -g) /root/.kube

echo "##################################"
echo "\n"
echo "the installation of k8s is over!!"
echo "\n"
echo "##################################"
fi
# 如果命令中参数携带有参数 -c 则执行Cas安装
#################OPEN-CAS###############

if [ "$isCAS" == "1" ];then
echo "###########################################"
echo "\n"
echo "==the next is the openCAS installation==\n"
echo "\n"
echo "###########################################"
disk1=`ls -l  /dev/disk/by-id|sed -n  4p |xargs|awk '{print $9}' `
disk2=`ls -l  /dev/disk/by-id|sed -n  5p |xargs|awk '{print $9}'`
wget https://github.com/Open-CAS/open-cas-linux/releases/download/v21.6.1/open-cas-linux-21.06.1.0547.release.tar.gz
tar -xf open-cas-linux-21.06.1.0547.release.tar.gz
cd open-cas-linux-21.06.1.0547.release/
./configure
make
make install
echo "disk1: $disk1"
echo "disk2: $disk2"
casadm -S -d /dev/disk/by-id/$disk1
casadm -A -d /dev/disk/by-id/$disk2 -i 1
echo "install openCAS finished!"
casadm -L
fi
exit 0
