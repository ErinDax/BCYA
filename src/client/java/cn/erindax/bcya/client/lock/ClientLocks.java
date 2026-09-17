package cn.erindax.bcya.client.lock;

import cn.erindax.bcya.lock.LockType;
import cn.erindax.bcya.lock.net.LockSyncPayload;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;

public final class ClientLocks {

	private static final Map<BlockPos, LockType> LOCKS = new HashMap<>();

	private ClientLocks() {
	}

	public static Map<BlockPos, LockType> all() {
		return LOCKS;
	}

	public static void replace(LockSyncPayload payload) {
		LOCKS.clear();
		for (LockSyncPayload.Entry entry : payload.entries()) {
			LOCKS.put(entry.pos(), entry.type());
		}
	}

	public static void update(BlockPos pos, int type) {
		if (type < 0) {
			LOCKS.remove(pos);
		} else {
			LOCKS.put(pos, LockType.byIndex(type));
		}
	}

	public static void clear() {
		LOCKS.clear();
	}
}
