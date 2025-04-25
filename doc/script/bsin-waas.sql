/*
 Navicat Premium Dump SQL

 Source Server         : yue17
 Source Server Type    : MySQL
 Source Server Version : 50744 (5.7.44-log)
 Source Host           : rm-2zeq9708apq292r36ko.mysql.rds.aliyuncs.com:3306
 Source Schema         : yue17-waas

 Target Server Type    : MySQL
 Target Server Version : 50744 (5.7.44-log)
 File Encoding         : 65001

 Date: 25/04/2025 15:24:31
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for waas_black_white_list_address
-- ----------------------------
DROP TABLE IF EXISTS `waas_black_white_list_address`;
CREATE TABLE `waas_black_white_list_address` (
  `address_id` varchar(32) NOT NULL COMMENT '地址id',
  `address` varchar(100) NOT NULL COMMENT '地址',
  `coin_id` varchar(100) NOT NULL COMMENT '币种ID',
  `status` int(4) NOT NULL COMMENT '状态;1、启用 2、禁用',
  `type` int(4) NOT NULL COMMENT '类型;0、白名单 1、黑名单',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `merchant_id` varchar(32) NOT NULL COMMENT '商户id',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` int(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除;0、未删除 1、已删除',
  PRIMARY KEY (`address_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='地址黑白名单;';

-- ----------------------------
-- Records of waas_black_white_list_address
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for waas_chain_coin
-- ----------------------------
DROP TABLE IF EXISTS `waas_chain_coin`;
CREATE TABLE `waas_chain_coin` (
  `serial_no` varchar(32) NOT NULL COMMENT '链上货币id;雪花算法',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户ID',
  `chain_coin_key` varchar(64) NOT NULL COMMENT '链上货币key',
  `chain_coin_name` varchar(128) NOT NULL COMMENT '链上货币名称',
  `short_name` varchar(64) DEFAULT NULL COMMENT '链上货币简称',
  `coin` varchar(32) NOT NULL COMMENT '币种',
  `chain_name` varchar(100) NOT NULL COMMENT '链名',
  `contract_address` varchar(255) DEFAULT NULL COMMENT '合约地址',
  `coin_decimal` decimal(10,0) NOT NULL COMMENT '币种精度',
  `unit` varchar(64) NOT NULL COMMENT '单位',
  `status` int(4) NOT NULL COMMENT '状态;0、下架 1、上架',
  `type` int(4) DEFAULT NULL COMMENT '类型;1、通用 2、自定义',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `logo_url` varchar(255) DEFAULT NULL COMMENT '币种logo',
  `biz_role_type` varchar(255) DEFAULT NULL COMMENT '所属业务角色类型',
  `biz_role_type_no` varchar(32) DEFAULT NULL COMMENT '所属业务角色类型编号',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` int(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除;0、未删除 1、已删除',
  PRIMARY KEY (`serial_no`) USING BTREE,
  UNIQUE KEY `chain_coin_key_unique` (`chain_coin_key`) USING BTREE,
  UNIQUE KEY `coin_chain_name_unique` (`coin`,`chain_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='币种表';


-- ----------------------------
-- Table structure for waas_contract
-- ----------------------------
DROP TABLE IF EXISTS `waas_contract`;
CREATE TABLE `waas_contract` (
  `serial_no` varchar(32) NOT NULL COMMENT '合约配置编号',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户id',
  `merchant_no` varchar(32) DEFAULT NULL COMMENT '商户NO',
  `tx_hash` varchar(255) DEFAULT NULL COMMENT '交易hash',
  `description` varchar(100) DEFAULT NULL COMMENT '规则描述',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `status` int(11) DEFAULT '0' COMMENT '是否被执行 0、未执行 1、执行中 2、已完成',
  `del_flag` int(11) DEFAULT '0' COMMENT '逻辑删除 0、未删除 1、已删除',
  `contract` varchar(255) DEFAULT NULL COMMENT '合约地址',
  `contract_protocol_no` varchar(32) DEFAULT NULL COMMENT '合约协议号',
  `chain_env` varchar(255) DEFAULT NULL COMMENT '链环境',
  `chain_type` varchar(255) DEFAULT NULL COMMENT '链类型',
  `name` varchar(255) DEFAULT NULL COMMENT '合约名称',
  `version` varchar(255) DEFAULT NULL COMMENT '版本号：010203',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='合约协议';


-- ----------------------------
-- Table structure for waas_contract_method
-- ----------------------------
DROP TABLE IF EXISTS `waas_contract_method`;
CREATE TABLE `waas_contract_method` (
  `method_name` varchar(120) DEFAULT NULL COMMENT '合约地址',
  `method_id` varchar(10) DEFAULT NULL,
  `type` int(11) DEFAULT NULL COMMENT '方法类型'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='合约方法表';

-- ----------------------------
-- Records of waas_contract_method
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for waas_contract_protocol
-- ----------------------------
DROP TABLE IF EXISTS `waas_contract_protocol`;
CREATE TABLE `waas_contract_protocol` (
  `serial_no` varchar(32) NOT NULL COMMENT '合约序列号',
  `protocol_name` varchar(64) NOT NULL COMMENT '合约名称',
  `protocol_code` varchar(255) NOT NULL COMMENT '合约项目编号：项目编号-协议-用途(bigan-erc721-pfp)',
  `protocol_standards` varchar(255) DEFAULT NULL COMMENT '合约协议标准 ：1、ERC20 2、ERC721 3、ERC1155 4、ERC6551 5、DaoBookCore 6、DaoBookFactory 7、DaoBookExtension 8、DaoBookWrapper',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户id',
  `type` varchar(11) DEFAULT NULL COMMENT '合约类型: 1、数字徽章 2、PFP 3、数字积分 4、数字门票 5、pass卡 6、徽章/门票',
  `protocol_bytecode` longtext COMMENT '合约模板bytecode',
  `protocol_abi` longtext COMMENT '合约模板abi字符',
  `description` varchar(100) DEFAULT NULL COMMENT '模板描述',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建者',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `del_flag` int(11) DEFAULT '0' COMMENT '逻辑删除 0、未删除 1、已删除',
  `chain_type` varchar(255) DEFAULT NULL COMMENT '链类型：conflux|polygon|ethereum|tron|bsc|evm',
  `category` varchar(11) DEFAULT NULL COMMENT '合约分类： 1、Core 2、Factory 3、Extension 4、Wrapper  5、Proxy  6、Other',
  `version` varchar(255) DEFAULT NULL COMMENT '版本号：010203',
  `cover_image` varchar(255) DEFAULT NULL COMMENT '合约封面图面',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='合约协议';


-- ----------------------------
-- Table structure for waas_customer_chain_coin
-- ----------------------------
DROP TABLE IF EXISTS `waas_customer_chain_coin`;
CREATE TABLE `waas_customer_chain_coin` (
  `serial_no` varchar(32) DEFAULT NULL,
  `del_flag` int(11) DEFAULT NULL,
  `create_role_account_flag` int(11) DEFAULT NULL,
  `create_user_account_flag` int(11) DEFAULT NULL,
  `biz_role_type_no` int(11) DEFAULT NULL,
  `biz_role_type` int(11) DEFAULT NULL,
  `tenant_id` int(11) DEFAULT NULL,
  `chain_coin_no` varchar(32) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- ----------------------------
-- Table structure for waas_customer_digital_assets
-- ----------------------------
DROP TABLE IF EXISTS `waas_customer_digital_assets`;
CREATE TABLE `waas_customer_digital_assets` (
  `serial_no` varchar(32) NOT NULL COMMENT '客户资产编号',
  `customer_no` varchar(32) DEFAULT NULL COMMENT '客户编号',
  `digital_assets_item_no` varchar(32) DEFAULT NULL COMMENT '客户数字资产编号',
  `token_id` int(11) DEFAULT NULL COMMENT '资产编号',
  `amount` int(32) NOT NULL DEFAULT '0' COMMENT '持有数量',
  `merchant_no` varchar(32) DEFAULT NULL COMMENT '商户号',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户id',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='客户的数字资产';


-- ----------------------------
-- Table structure for waas_customer_pass_card
-- ----------------------------
DROP TABLE IF EXISTS `waas_customer_pass_card`;
CREATE TABLE `waas_customer_pass_card` (
  `serial_no` varchar(32) NOT NULL COMMENT '序列号',
  `digital_assets_item_no` varchar(32) DEFAULT NULL COMMENT '客户数字资产编号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户',
  `customer_no` varchar(32) DEFAULT NULL COMMENT '客户号',
  `token_id` int(64) DEFAULT NULL COMMENT '卡号',
  `merchant_no` varchar(32) DEFAULT NULL COMMENT '商户号',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态',
  `tba_address` varchar(255) DEFAULT NULL COMMENT 'TBA账户地址：会员卡是一个ERC6551协议的TBA账户，一张会员卡一个TBA账户',
  `amount` int(64) DEFAULT NULL COMMENT '数量',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员通行证表';


-- ----------------------------
-- Table structure for waas_customer_profile
-- ----------------------------
DROP TABLE IF EXISTS `waas_customer_profile`;
CREATE TABLE `waas_customer_profile` (
  `serial_no` varchar(32) NOT NULL COMMENT 'profile ID',
  `merchant_no` varchar(32) NOT NULL COMMENT '商户ID',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户id',
  `customer_no` varchar(32) NOT NULL COMMENT '客户号',
  `type` varchar(11) DEFAULT NULL COMMENT 'profile分类： Brand|Individual',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `del_flag` int(11) DEFAULT '0' COMMENT '逻辑删除 0、未删除 1、已删除',
  `description` longtext COMMENT '介绍',
  `contract_address` varchar(64) NOT NULL COMMENT 'profile合约地址',
  `chain_env` varchar(32) DEFAULT NULL COMMENT '链网络： main|test',
  `chain_type` varchar(32) DEFAULT NULL COMMENT '链类型：conflux|polygon|ethereum|tron|bsc|evm',
  `external_uri` varchar(255) DEFAULT NULL COMMENT 'profile external URI for profile metadata',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `name` varchar(255) DEFAULT NULL COMMENT 'profile名称',
  `symbol` varchar(255) DEFAULT NULL COMMENT 'profile符号',
  `update_by` varchar(32) DEFAULT NULL,
  `member_no` int(32) DEFAULT NULL COMMENT '会员数量',
  `profile_num` int(32) DEFAULT NULL COMMENT 'profile的编号：根据创建时间从0递增',
  `assets_count` int(32) DEFAULT NULL COMMENT 'assets的数量：发行和注册搭配profile中的资产数量',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='品牌和超级个体profile记录表';


-- ----------------------------
-- Table structure for waas_digital_assets_collection
-- ----------------------------
DROP TABLE IF EXISTS `waas_digital_assets_collection`;
CREATE TABLE `waas_digital_assets_collection` (
  `serial_no` varchar(32) NOT NULL COMMENT '集合编号',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户id',
  `merchant_no` varchar(32) DEFAULT NULL COMMENT '品牌商户号',
  `symbol` varchar(255) DEFAULT NULL COMMENT '英文符号',
  `name` varchar(32) DEFAULT NULL COMMENT '资产名称',
  `total_supply` decimal(32,0) NOT NULL DEFAULT '0' COMMENT '总供应量',
  `decimals` decimal(11,0) NOT NULL DEFAULT '0' COMMENT '小数位数',
  `collection_type` varchar(255) DEFAULT NULL COMMENT '品牌商户发行资产类型 1、数字徽章 2、PFP 3、数字积分 4、数字门票 5、pass卡 ',
  `status` int(11) DEFAULT '0' COMMENT '市场流通状态 0、未流通 1、流通中 2、流通完成',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `contract_address` varchar(255) NOT NULL COMMENT '合约地址',
  `metadata_image_same_flag` varchar(255) DEFAULT NULL COMMENT '图片是否相同标识',
  `metadata_file_path_no` varchar(32) DEFAULT NULL COMMENT '元数据文件ipfs目录',
  `metadata_template_no` varchar(32) DEFAULT NULL COMMENT '元数据模板编号',
  `del_flag` int(11) DEFAULT '0' COMMENT '逻辑删除 0、未删除 1、已删除',
  `bonding_curve_flag` varchar(255) DEFAULT '0' COMMENT '是否是基于联合取消铸造：0 否 1是',
  `contract_protocol_no` varchar(255) NOT NULL DEFAULT '' COMMENT '合约协议:',
  `chain_env` varchar(255) NOT NULL COMMENT '链网络环境',
  `chain_type` varchar(255) NOT NULL COMMENT '链',
  `sponsor_flag` varchar(255) DEFAULT '0' COMMENT '是否被赞助 0  否 1是',
  `inventory` decimal(32,0) NOT NULL DEFAULT '0' COMMENT '库存：还可上架的数量',
  `initial_supply` decimal(32,0) NOT NULL DEFAULT '0' COMMENT '初始供应量',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数字资产集合';


-- ----------------------------
-- Table structure for waas_digital_assets_item
-- ----------------------------
DROP TABLE IF EXISTS `waas_digital_assets_item`;
CREATE TABLE `waas_digital_assets_item` (
  `serial_no` varchar(32) NOT NULL COMMENT '序列编号',
  `digital_assets_collection_no` varchar(32) DEFAULT NULL COMMENT '数字资产集合编号',
  `assets_type` varchar(255) DEFAULT NULL COMMENT '1、数字徽章(ERC1155) 2、PFP(ERC71） 3、积分(ERC20) 4、门票(ERC1155)  5、pass卡(ERC1155) ',
  `assets_name` varchar(32) DEFAULT NULL COMMENT 'NFT资产名称',
  `description` text COMMENT '描述',
  `multimedia_type` varchar(10) DEFAULT NULL COMMENT '多媒体类型： 1 图片  2 gif 3 视频 4 音频 5 json 6文件夹',
  `cover_image` varchar(1024) DEFAULT NULL COMMENT '封面图片',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '价格',
  `quantity` decimal(11,0) NOT NULL DEFAULT '0' COMMENT '数量：上架的数量，721每次上架会增加，不会减少(流通量)',
  `obtain_method` varchar(255) DEFAULT NULL COMMENT '领取方式：1 免费领取/空投 2 购买  3 固定口令领取 4 随机口令 5 盲盒 6 活动',
  `inventory` decimal(11,0) NOT NULL DEFAULT '0' COMMENT '库存：每次上架会增加，领取会减少',
  `on_sell` varchar(11) DEFAULT NULL COMMENT '是否在售 0、是 1、否',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '修改人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `merchant_no` varchar(32) DEFAULT NULL COMMENT '商户号',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户id',
  `del_flag` int(1) DEFAULT '0' COMMENT '逻辑删除 0、未删除 1、已删除',
  `token_id` int(11) DEFAULT '0' COMMENT '1155协议默认上架有tokenId',
  `chain_type` varchar(32) DEFAULT NULL,
  `chain_env` varchar(32) DEFAULT NULL,
  `current_mint_token_id` decimal(11,0) NOT NULL DEFAULT '1' COMMENT '当前铸造的tokenId编号',
  `metadata_url` varchar(255) DEFAULT NULL COMMENT '元数据url',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数字资产';


-- ----------------------------
-- Table structure for waas_digital_assets_item_obtain_code
-- ----------------------------
DROP TABLE IF EXISTS `waas_digital_assets_item_obtain_code`;
CREATE TABLE `waas_digital_assets_item_obtain_code` (
  `serial_no` varchar(32) NOT NULL,
  `assets_no` varchar(32) DEFAULT NULL COMMENT 'NFT编号',
  `password` varchar(255) DEFAULT NULL COMMENT '领取口令',
  `status` varchar(255) DEFAULT '1' COMMENT '领取状态：1 待领取 2 已领取',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `mint_no` varchar(11) DEFAULT NULL COMMENT '铸造编号',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数字资产领取口令';


-- ----------------------------
-- Table structure for waas_merchant_coin_relations
-- ----------------------------
DROP TABLE IF EXISTS `waas_merchant_coin_relations`;
CREATE TABLE `waas_merchant_coin_relations` (
  `coin_id` varchar(32) NOT NULL COMMENT '币种ID',
  `merchant_id` varchar(32) NOT NULL COMMENT '商户ID',
  PRIMARY KEY (`coin_id`,`merchant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='商户币种关联表;';

-- ----------------------------
-- Records of waas_merchant_coin_relations
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for waas_merchant_profile
-- ----------------------------
DROP TABLE IF EXISTS `waas_merchant_profile`;
CREATE TABLE `waas_merchant_profile` (
  `serial_no` varchar(32) NOT NULL COMMENT 'profile ID',
  `merchant_no` varchar(32) NOT NULL COMMENT '商户ID',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户id',
  `customer_no` varchar(32) NOT NULL COMMENT '客户号',
  `type` varchar(11) DEFAULT NULL COMMENT 'profile分类： Brand|Individual',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `del_flag` int(11) DEFAULT '0' COMMENT '逻辑删除 0、未删除 1、已删除',
  `description` longtext COMMENT '介绍',
  `contract_address` varchar(64) NOT NULL COMMENT 'profile合约地址',
  `chain_env` varchar(32) DEFAULT NULL COMMENT '链网络： main|test',
  `chain_type` varchar(32) DEFAULT NULL COMMENT '链类型：conflux|polygon|ethereum|tron|bsc|evm',
  `external_uri` varchar(255) DEFAULT NULL COMMENT 'profile external URI for profile metadata',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `name` varchar(255) DEFAULT NULL COMMENT 'profile名称',
  `symbol` varchar(255) DEFAULT NULL COMMENT 'profile符号',
  `update_by` varchar(32) DEFAULT NULL,
  `member_no` int(32) DEFAULT NULL COMMENT '会员数量',
  `profile_num` int(32) DEFAULT NULL COMMENT 'profile的编号：根据创建时间从0递增',
  `assets_count` int(32) DEFAULT NULL COMMENT 'assets的数量：发行和注册搭配profile中的资产数量',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='品牌和超级个体profile记录表';

-- ----------------------------
-- Records of waas_merchant_profile
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for waas_metadata_file
-- ----------------------------
DROP TABLE IF EXISTS `waas_metadata_file`;
CREATE TABLE `waas_metadata_file` (
  `serial_no` varchar(32) DEFAULT NULL COMMENT '文件ID',
  `token_id` int(11) DEFAULT NULL COMMENT 'tokenId',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '所属租户',
  `merchant_no` varchar(32) DEFAULT NULL COMMENT '所属商户',
  `parent_no` varchar(32) DEFAULT '0' COMMENT '父级ID',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文件名称',
  `file_code` varchar(255) DEFAULT NULL COMMENT '文件编号',
  `file_description` varchar(255) DEFAULT NULL COMMENT '描述',
  `file_type` varchar(255) DEFAULT NULL COMMENT '文件类型： 1 图片  2 gif 3 视频 4 音频 5 json 6文件夹',
  `dir_flag` varchar(255) DEFAULT '0' COMMENT '是否是目录 0 否 1是',
  `file_url` varchar(255) DEFAULT NULL COMMENT '文件地址',
  `create_by` varchar(255) DEFAULT NULL COMMENT '上传用户号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `metadata_content` text COMMENT 'metadata json内容',
  `ipfs_url` varchar(255) DEFAULT NULL COMMENT 'ipfs url'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='元数据文件';


-- ----------------------------
-- Table structure for waas_metadata_template
-- ----------------------------
DROP TABLE IF EXISTS `waas_metadata_template`;
CREATE TABLE `waas_metadata_template` (
  `serial_no` varchar(64) NOT NULL COMMENT '模板编号',
  `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户ID',
  `merchant_no` varchar(64) DEFAULT NULL COMMENT '商户号',
  `template_content` longtext COMMENT '模板数据',
  `template_name` varchar(255) DEFAULT NULL COMMENT '模板名称',
  `description` text COMMENT '描述',
  `template_code` varchar(255) DEFAULT NULL COMMENT '模板代号：？？？',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `del_flag` int(1) DEFAULT '0' COMMENT '逻辑删除 0、未删除 1、已删除',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='元数据模板';


-- ----------------------------
-- Table structure for waas_mint_journal
-- ----------------------------
DROP TABLE IF EXISTS `waas_mint_journal`;
CREATE TABLE `waas_mint_journal` (
  `serial_no` varchar(32) NOT NULL COMMENT 'mint编号',
  `digital_assets_collection_no` varchar(32) DEFAULT NULL COMMENT '数字资产集合编号',
  `digital_assets_item_no` varchar(32) DEFAULT NULL COMMENT '数字资产编号',
  `token_id` varchar(32) DEFAULT NULL COMMENT '链上唯一标识',
  `amount` decimal(32,0) DEFAULT NULL COMMENT '铸造数量',
  `multimedia_type` varchar(10) DEFAULT NULL COMMENT '多媒体类型： 1 图片  2 gif 3 视频 4 音频 5 json 6 文件夹',
  `metadata_image` varchar(255) DEFAULT NULL COMMENT 'mint出的nft图片地址',
  `metadata_url` varchar(100) DEFAULT NULL COMMENT 'metadata地址',
  `tx_hash` varchar(255) DEFAULT NULL COMMENT '交易哈希',
  `merchant_no` varchar(32) DEFAULT NULL COMMENT '商户号',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户ID',
  `to_customer_no` varchar(255) DEFAULT NULL COMMENT '铸造客户号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '铸造时间',
  `to_phone` varchar(255) DEFAULT NULL COMMENT '铸造人手机号',
  `to_minter_name` varchar(255) DEFAULT NULL COMMENT '铸造人姓名',
  `to_address` varchar(255) DEFAULT NULL COMMENT '铸造人地址',
  `chain_env` varchar(255) DEFAULT NULL COMMENT '链网络环境',
  `item_name` text COMMENT 'NFT名称',
  `assets_type` varchar(255) DEFAULT NULL COMMENT '资产类型：1、数字徽章 2、PFP 3、积分 4、门票 5、pass卡',
  `chain_type` varchar(32) DEFAULT NULL COMMENT '链网络',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数字资产铸造流水';


-- ----------------------------
-- Table structure for waas_orderbook
-- ----------------------------
DROP TABLE IF EXISTS `waas_orderbook`;
CREATE TABLE `waas_orderbook` (
  `serial_no` varchar(64) NOT NULL COMMENT '订单编号',
  `tenant_id` varchar(64) DEFAULT NULL COMMENT '租户ID',
  `merchant_no` varchar(64) DEFAULT NULL COMMENT '资产商户号',
  `from_assets_type` varchar(1) DEFAULT NULL COMMENT '卖方资产类型：1：NFT、2：FT',
  `from_customer_no` varchar(64) DEFAULT NULL COMMENT '客户号',
  `customer_type` varchar(1) DEFAULT NULL COMMENT '客户类型：1 个人  2商户',
  `status` int(1) DEFAULT '0' COMMENT '订单状态 1、待交易 2、部分交易 3、完成交易 4、已撤单 5、部分交易并撤单',
  `from_token_id` int(11) DEFAULT NULL COMMENT '来源数字资产tokenId',
  `from_digital_assets_no` varchar(64) DEFAULT NULL COMMENT '来源数字资产编号',
  `from_amount` decimal(10,2) DEFAULT NULL COMMENT '资产数量',
  `to_digital_assets_no` varchar(64) DEFAULT NULL COMMENT '目标数字资产编号',
  `to_assets_type` varchar(255) DEFAULT NULL COMMENT '买方资产类型：1：NFT、2：FT 进行积分入账',
  `to_amount` decimal(10,2) DEFAULT NULL COMMENT '资产数量',
  `exchange_rate` decimal(10,2) DEFAULT NULL COMMENT '汇率',
  `exchanged_amount` decimal(10,2) DEFAULT NULL COMMENT '已经兑换数量',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数字资产交易订单';

-- ----------------------------
-- Records of waas_orderbook
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for waas_orderbook_match_journal
-- ----------------------------
DROP TABLE IF EXISTS `waas_orderbook_match_journal`;
CREATE TABLE `waas_orderbook_match_journal` (
  `serial_no` varchar(64) NOT NULL COMMENT '订单编号',
  `orderbook_no` varchar(64) DEFAULT NULL COMMENT '订单号',
  `status` int(1) DEFAULT '0' COMMENT '订单状态 0、失败 1、成功 ',
  `amount` varchar(64) DEFAULT NULL COMMENT '资产数量',
  `to_customer_no` varchar(64) DEFAULT NULL COMMENT '兑换的客户号',
  `to_customer_type` varchar(255) DEFAULT NULL COMMENT '兑换的客户类型：1 个人客户 2商户客户',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数字资产交易流水';

-- ----------------------------
-- Records of waas_orderbook_match_journal
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for waas_pay_channel_config
-- ----------------------------
DROP TABLE IF EXISTS `waas_pay_channel_config`;
CREATE TABLE `waas_pay_channel_config` (
  `serial_no` varchar(255) NOT NULL COMMENT 'ID',
  `tenant_id` varchar(32) NOT NULL,
  `biz_role_app_id` varchar(64) NOT NULL COMMENT '服务商号/商户号/应用ID',
  `pay_channel_code` varchar(20) NOT NULL COMMENT '支付通道接口代码',
  `params` varchar(4096) NOT NULL COMMENT '接口配置参数：支付渠道的appId等，json字符串，从 pay_channel_intrface 中的params查询出字段名称并进行赋值，存储',
  `fee_ratio` decimal(20,6) NOT NULL DEFAULT '0.000000' COMMENT '支付接口费率',
  `status` varchar(6) NOT NULL DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
  `remark` varchar(128) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者姓名',
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者姓名',
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用支付接口参数配置表';


-- ----------------------------
-- Table structure for waas_pay_channel_interface
-- ----------------------------
DROP TABLE IF EXISTS `waas_pay_channel_interface`;
CREATE TABLE `waas_pay_channel_interface` (
  `tenant_id` varchar(255) NOT NULL,
  `pay_channel_code` varchar(20) NOT NULL COMMENT '支付通道代码：全小写  wxpay alipay ',
  `pay_channel_name` varchar(20) NOT NULL COMMENT '支付通道名称：微信支付 支付宝支付',
  `config_page_type` varchar(6) NOT NULL DEFAULT '1' COMMENT '支付参数配置页面类型:1-JSON渲染,2-自定义',
  `params` json DEFAULT NULL COMMENT '普通商户接口配置定义描述：json字符串',
  `way_code` varchar(255) NOT NULL COMMENT '支持的支付方式 ["WX_JSAPI", "WX_H5", "WX_APP", "ALI_BAR", "ALI_APP", "ALI_WAP"]',
  `icon` varchar(256) DEFAULT NULL COMMENT '页面展示：卡片-图标',
  `status` tinyint(6) NOT NULL DEFAULT '1' COMMENT '状态: 0-停用, 1-启用',
  `remark` varchar(128) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`pay_channel_code`,`tenant_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付渠道具体接口定义表';

-- ----------------------------
-- Table structure for waas_pay_way
-- ----------------------------
DROP TABLE IF EXISTS `waas_pay_way`;
CREATE TABLE `waas_pay_way` (
  `serial_no` varchar(255) NOT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  `pay_way_name` varchar(20) NOT NULL COMMENT '支付渠道名称：微信JSAPI支付 微信APP支付 微信H5支付 微信Native支付 微信小程序支付',
  `pay_way_code` varchar(20) NOT NULL COMMENT '支付方式代码  例如： WX_JSAPI", "WX_H5", "WX_APP", "ALI_BAR", "ALI_APP", "ALI_WAP“',
  `create_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `update_time` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`serial_no`) USING BTREE,
  UNIQUE KEY `pay_way_code` (`pay_way_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付渠道表';

-- ----------------------------
-- Table structure for waas_token_param
-- ----------------------------
DROP TABLE IF EXISTS `waas_token_param`;
CREATE TABLE `waas_token_param` (
  `serial_no` varchar(255) NOT NULL COMMENT 'ID',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户ID',
  `merchant_no` varchar(32) DEFAULT NULL COMMENT '商户号',
  `digital_assets_collection_no` varchar(32) DEFAULT NULL COMMENT '数字资产集合',
  `symbol` varchar(255) DEFAULT NULL COMMENT '币种符号',
  `name` varchar(255) DEFAULT NULL COMMENT '名称',
  `total_supply` decimal(65,0) DEFAULT NULL COMMENT '发行方总供应量',
  `decimals` decimal(11,0) NOT NULL DEFAULT '0' COMMENT '小数点位数',
  `reserved_amount` decimal(10,2) DEFAULT '0.00' COMMENT '释放预留量：用于任务|活动参与的质押',
  `capture_total_value` decimal(65,0) DEFAULT '0' COMMENT '预估捕获的总劳动价值',
  `release_method` varchar(255) DEFAULT '1' COMMENT '释放方式：1、曲线价值释放， 2、购买释放， 3、周期释放',
  `unit_release_trigger_value` decimal(65,0) DEFAULT NULL COMMENT '单元释放的触发价值，每累计达到释放一次',
  `unit_release_amout` decimal(65,0) DEFAULT NULL COMMENT '单元释放的token数量',
  `release_cycle` decimal(65,0) DEFAULT NULL COMMENT '释放周期：单位天',
  `issue_method` varchar(255) DEFAULT NULL COMMENT '发行方式：1 购买发行、2 自定义发行',
  `issuer_type` varchar(255) DEFAULT NULL COMMENT '发行方类型：1、平台 2、租户 3、商户',
  `status` varchar(1) DEFAULT '0' COMMENT '状态：0 未启用 1 启用',
  `anchoring_value` decimal(10,2) NOT NULL DEFAULT '1.00' COMMENT '锚定法币价值',
  `circulation` decimal(65,0) DEFAULT NULL COMMENT '流通量',
  `description` text COMMENT '描述',
  `exchange_rate` decimal(11,2) NOT NULL DEFAULT '1.00' COMMENT '释放兑换比例：数字积分：联合曲线--> 数字积分=bc*exchangeRate',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数字积分参数配置';

-- ----------------------------
-- Table structure for waas_token_release_journal
-- ----------------------------
DROP TABLE IF EXISTS `waas_token_release_journal`;
CREATE TABLE `waas_token_release_journal` (
  `serial_no` varchar(32) NOT NULL,
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户号',
  `merchant_no` varchar(32) DEFAULT NULL COMMENT '商户号',
  `customer_no` varchar(32) DEFAULT NULL COMMENT '客户号',
  `amout` decimal(32,0) DEFAULT NULL COMMENT '分配数量',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数字资产释放流水';

-- ----------------------------
-- Records of waas_token_release_journal
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for waas_transaction
-- ----------------------------
DROP TABLE IF EXISTS `waas_transaction`;
CREATE TABLE `waas_transaction` (
  `serial_no` varchar(32) NOT NULL COMMENT '交易id',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户',
  `biz_role_type` varchar(10) DEFAULT NULL COMMENT '业务角色类型;1、平台 2、商户 3、代理商 4、用户',
  `biz_role_type_no` varchar(32) DEFAULT NULL COMMENT '业务角色类型编号',
  `tx_hash` varchar(255) DEFAULT NULL COMMENT '交易hash',
  `out_serial_no` varchar(255) DEFAULT NULL COMMENT '商户业务唯一标识',
  `transaction_type` varchar(4) NOT NULL COMMENT '交易类型：见枚举',
  `transaction_status` varchar(4) NOT NULL DEFAULT '3' COMMENT '交易状态：TransactionStatus: 1、等待 2、成功 3、失败',
  `contract_address` varchar(255) DEFAULT NULL COMMENT '合约地址',
  `contract_method` varchar(255) DEFAULT NULL COMMENT '执行的合约方法',
  `method_invoke_way` int(11) DEFAULT NULL,
  `tx_amount` decimal(36,6) NOT NULL COMMENT '交易金额',
  `fee` decimal(24,6) DEFAULT NULL COMMENT '手续费',
  `gas_fee` decimal(24,6) DEFAULT NULL COMMENT '实际gas费用',
  `from_address_type` varchar(255) DEFAULT NULL COMMENT '源地址类型',
  `from_address` varchar(255) DEFAULT NULL COMMENT '源地址',
  `to_address_type` varchar(255) DEFAULT NULL COMMENT '目标地址类型',
  `to_address` varchar(255) DEFAULT NULL COMMENT '目标地址',
  `comment` varchar(255) DEFAULT NULL COMMENT '备注',
  `audit_status` varchar(255) DEFAULT NULL COMMENT '审核状态;1、待审核 2、审核成功 3、审核失败',
  `completed_time` datetime DEFAULT NULL COMMENT '交易完成时间',
  `merchant_id` varchar(255) DEFAULT NULL COMMENT '商户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(255) DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(255) DEFAULT NULL COMMENT '更新人',
  `del_flag` int(11) DEFAULT '0' COMMENT '逻辑删除;0、未删除 1、已删除',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='交易记录;';

-- ----------------------------
-- Table structure for waas_transaction_audit
-- ----------------------------
DROP TABLE IF EXISTS `waas_transaction_audit`;
CREATE TABLE `waas_transaction_audit` (
  `transaction_audit_id` varchar(32) NOT NULL COMMENT '交易审核id',
  `audit_ype` int(11) NOT NULL COMMENT '审核类型;1、交易转出审核',
  `audit_status` int(11) NOT NULL COMMENT '审核状态;1、待审核 2、审核通过 3、审核拒绝',
  `audit_level` int(11) NOT NULL COMMENT '审核级别;1、一级 2、二级 3、三级',
  `transaction_id` int(11) NOT NULL COMMENT '交易id',
  `user_id` int(11) DEFAULT NULL COMMENT '审核人（平台用户）',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `reason` varchar(255) DEFAULT NULL COMMENT '原因',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` int(11) NOT NULL COMMENT '逻辑删除;0、未删除 1、已删除',
  PRIMARY KEY (`transaction_audit_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='交易审核表;';

-- ----------------------------
-- Records of waas_transaction_audit
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for waas_transaction_journal
-- ----------------------------
DROP TABLE IF EXISTS `waas_transaction_journal`;
CREATE TABLE `waas_transaction_journal` (
  `serial_no` varchar(32) NOT NULL,
  `pay_way_no` varchar(32) DEFAULT NULL,
  `pay_amount` varchar(255) DEFAULT NULL,
  `pay_fee` varchar(255) DEFAULT NULL,
  `transaction_no` varchar(255) DEFAULT NULL COMMENT '交易单号',
  `status` varchar(255) DEFAULT NULL COMMENT '交易状态：TransactionStatus: 1、等待 2、成功 3、失败',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='交易流水表';

-- ----------------------------
-- Table structure for waas_transfer_journal
-- ----------------------------
DROP TABLE IF EXISTS `waas_transfer_journal`;
CREATE TABLE `waas_transfer_journal` (
  `serial_no` varchar(32) NOT NULL COMMENT 'transfer编号',
  `digital_assets_collection_no` varchar(32) DEFAULT NULL COMMENT '数字资产编号',
  `token_id` varchar(32) DEFAULT NULL COMMENT '链上唯一标识',
  `amount` int(11) DEFAULT NULL COMMENT '转移数量',
  `metadata_image` varchar(255) DEFAULT NULL COMMENT 'mint出的nft图片地址',
  `metadata_url` varchar(100) DEFAULT NULL COMMENT 'metadata地址',
  `tx_hash` varchar(255) DEFAULT NULL COMMENT '交易哈希',
  `tenant_id` varchar(32) DEFAULT NULL COMMENT '租户ID',
  `merchant_no` varchar(32) DEFAULT NULL COMMENT '资产商户编号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `from_customer_no` varchar(255) DEFAULT NULL COMMENT '转出客户号',
  `from_address` varchar(255) DEFAULT NULL COMMENT '源地址',
  `to_address` varchar(255) DEFAULT NULL COMMENT '目标地址',
  `to_name` varchar(255) DEFAULT NULL COMMENT '接收人姓名',
  `to_phone` varchar(255) DEFAULT NULL COMMENT '铸造人手机号',
  `to_customer_no` varchar(255) DEFAULT NULL COMMENT '接受客户号',
  `chain_env` varchar(32) DEFAULT NULL COMMENT '链网络环境',
  `assets_type` varchar(255) DEFAULT NULL COMMENT '资产类型：1、数字徽章 2、PFP 3、积分 4、门票 5、pass卡',
  `chain_type` varchar(32) DEFAULT NULL COMMENT '链网络',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数字资产NFT转账流水';

-- ----------------------------
-- Records of waas_transfer_journal
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for waas_wallet
-- ----------------------------
DROP TABLE IF EXISTS `waas_wallet`;
CREATE TABLE `waas_wallet` (
  `serial_no` varchar(32) NOT NULL COMMENT '钱包id;雪花算法',
  `wallet_name` varchar(128) NOT NULL COMMENT '账户名称',
  `type` varchar(4) NOT NULL DEFAULT '1' COMMENT '类型;1、默认钱包 2、自定义钱包',
  `status` varchar(4) NOT NULL DEFAULT '1' COMMENT '状态;1、正常 2、冻结 3、注销',
  `biz_role_type` varchar(4) NOT NULL COMMENT '业务角色类型',
  `wallet_tag` varchar(8) NOT NULL COMMENT '账户标签;NONE、 无  DEPOSIT、寄存(用户资金归集)',
  `out_user_id` varchar(128) DEFAULT NULL COMMENT '外部用户标识',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `biz_role_type_no` varchar(32) NOT NULL COMMENT '因为角色编号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `del_flag` int(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除;0、未删除 1、已删除',
  `category` varchar(11) DEFAULT NULL COMMENT '钱包分类',
  `env` varchar(10) DEFAULT NULL,
  `balance` int(11) DEFAULT NULL,
  `tenant_id` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='钱包;';

-- ----------------------------
-- Table structure for waas_wallet_account
-- ----------------------------
DROP TABLE IF EXISTS `waas_wallet_account`;
CREATE TABLE `waas_wallet_account` (
  `serial_no` varchar(32) NOT NULL COMMENT '钱包账户id;雪花算法',
  `tenant_id` varchar(32) NOT NULL,
  `address` varchar(255) DEFAULT NULL COMMENT '链钱包地址',
  `pub_key` varchar(255) DEFAULT NULL COMMENT '钱包公钥',
  `chain_coin_no` varchar(32) NOT NULL COMMENT '币种id',
  `status` varchar(255) DEFAULT NULL,
  `balance` decimal(24,0) NOT NULL COMMENT '余额',
  `wallet_no` varchar(32) NOT NULL COMMENT '钱包id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` varchar(32) DEFAULT NULL COMMENT '更新人',
  `del_flag` int(4) NOT NULL DEFAULT '0' COMMENT '逻辑删除;0、未删除 1、已删除',
  PRIMARY KEY (`serial_no`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='钱包账户;';


SET FOREIGN_KEY_CHECKS = 1;
