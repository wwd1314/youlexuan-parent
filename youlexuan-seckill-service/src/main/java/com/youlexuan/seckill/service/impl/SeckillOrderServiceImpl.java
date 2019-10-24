package com.youlexuan.seckill.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.youlexuan.CONSTANT;
import com.youlexuan.entity.PageResult;
import com.youlexuan.mapper.TbSeckillGoodsMapper;
import com.youlexuan.mapper.TbSeckillOrderMapper;
import com.youlexuan.pojo.TbSeckillGoods;
import com.youlexuan.pojo.TbSeckillOrder;
import com.youlexuan.pojo.TbSeckillOrderExample;
import com.youlexuan.pojo.TbSeckillOrderExample.Criteria;
import com.youlexuan.secKill.service.SeckillOrderService;
import com.youlexuan.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;
import java.util.List;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

	@Autowired
	private TbSeckillOrderMapper seckillOrderMapper;
	@Autowired
	private TbSeckillGoodsMapper seckillGoodsMapper;

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private IdWorker idWorker;
	/**
	 * 查询全部
	 */
	@Override
	public List<TbSeckillOrder> findAll() {
		return seckillOrderMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbSeckillOrder> page=   (Page<TbSeckillOrder>) seckillOrderMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbSeckillOrder seckillOrder) {
		seckillOrderMapper.insert(seckillOrder);		
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbSeckillOrder seckillOrder){
		seckillOrderMapper.updateByPrimaryKey(seckillOrder);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbSeckillOrder findOne(Long id){
		return seckillOrderMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			seckillOrderMapper.deleteByPrimaryKey(id);
		}		
	}
	
	
		@Override
	public PageResult findPage(TbSeckillOrder seckillOrder, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		
		TbSeckillOrderExample example=new TbSeckillOrderExample();
		Criteria criteria = example.createCriteria();
		
		if(seckillOrder!=null){			
						if(seckillOrder.getUserId()!=null && seckillOrder.getUserId().length()>0){
				criteria.andUserIdLike("%"+seckillOrder.getUserId()+"%");
			}			if(seckillOrder.getSellerId()!=null && seckillOrder.getSellerId().length()>0){
				criteria.andSellerIdLike("%"+seckillOrder.getSellerId()+"%");
			}			if(seckillOrder.getStatus()!=null && seckillOrder.getStatus().length()>0){
				criteria.andStatusLike("%"+seckillOrder.getStatus()+"%");
			}			if(seckillOrder.getReceiverAddress()!=null && seckillOrder.getReceiverAddress().length()>0){
				criteria.andReceiverAddressLike("%"+seckillOrder.getReceiverAddress()+"%");
			}			if(seckillOrder.getReceiverMobile()!=null && seckillOrder.getReceiverMobile().length()>0){
				criteria.andReceiverMobileLike("%"+seckillOrder.getReceiverMobile()+"%");
			}			if(seckillOrder.getReceiver()!=null && seckillOrder.getReceiver().length()>0){
				criteria.andReceiverLike("%"+seckillOrder.getReceiver()+"%");
			}			if(seckillOrder.getTransactionId()!=null && seckillOrder.getTransactionId().length()>0){
				criteria.andTransactionIdLike("%"+seckillOrder.getTransactionId()+"%");
			}	
		}
		
		Page<TbSeckillOrder> page= (Page<TbSeckillOrder>)seckillOrderMapper.selectByExample(example);		
		return new PageResult(page.getTotal(), page.getResult());
	}

	@Override
	public void submitOrder(Long id, String userId) {
		TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).get(id);
		if (seckillGoods==null){
			throw  new RuntimeException("秒杀商品不存在");
		}

		if (seckillGoods.getStockCount()<=0){
			throw  new RuntimeException("商品已经高勤");
		}

		seckillGoods.setStockCount(seckillGoods.getStockCount()-1);
		redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).put(id,seckillGoods);
		if (seckillGoods.getStockCount()==0){
			seckillGoodsMapper.updateByPrimaryKey(seckillGoods);
			redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).delete(id);

		}

		TbSeckillOrder seckillOrder = new TbSeckillOrder();
		Long orderId = idWorker.nextId();
		seckillOrder.setId(orderId);
		seckillOrder.setCreateTime(new Date());
		seckillOrder.setMoney(seckillGoods.getCostPrice());//秒杀价格
		seckillOrder.setSeckillId(id);
		seckillOrder.setSellerId(seckillGoods.getSellerId());
		seckillOrder.setUserId(userId);//设置用户ID
		seckillOrder.setStatus("0");//状态

		redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_KEY).put(userId,seckillOrder);




	}

	@Override
	public TbSeckillOrder searchSckillOrderByUserId(String userId) {
		return (TbSeckillOrder) redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_KEY).get(userId);
	}

	@Override
	public void saveOrderFromRedisToDb(String userId, String trade_no) {
		TbSeckillOrder seckillOrder = (TbSeckillOrder) redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_KEY).get(userId);

		if (seckillOrder==null){
			throw new RuntimeException("该订单不存在");
		}


		seckillOrder.setTransactionId(trade_no);
		seckillOrder.setPayTime(new Date());
		seckillOrder.setStatus("1");

		seckillOrderMapper.insertSelective(seckillOrder);

		redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_KEY).delete(userId);


	}

	@Override
	public void resetOrderFromRedis(String userId) {
		TbSeckillOrder seckillOrder = (TbSeckillOrder) redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_KEY).get(userId);
		if (seckillOrder ==null){
			throw new RuntimeException("该打你滚蛋不存在");
		}
		redisTemplate.boundHashOps(CONSTANT.SECKILL_ORDER_KEY).delete(userId);

		TbSeckillGoods seckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).get(seckillOrder.getSellerId());

		if (seckillGoods!=null){
			seckillGoods.setStockCount(seckillGoods.getStockCount()+1);
			redisTemplate.boundHashOps(CONSTANT.SECKILL_GOODS_LIST_KEY).put(seckillOrder.getId(),seckillGoods);
		}

	}

}
