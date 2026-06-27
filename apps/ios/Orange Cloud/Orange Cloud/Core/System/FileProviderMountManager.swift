//
//  FileProviderMountManager.swift
//  Orange Cloud
//
//  主 App 侧：把某个 R2 桶注册为系统「文件」App 里的一个 NSFileProviderDomain
//  （挂载 / 卸载 / 查询是否已挂载）。真正的读写由 OrangeCloudFileProvider extension 承担。
//  domain identifier 用 [[FileProviderDomainID]] 编码，extension 据此还原凭证与 R2 目标。
//
//  仅在工程已包含 File Provider extension target 时生效；该框架对主 App 可用。
//

import Foundation
import FileProvider

@MainActor
enum FileProviderMountManager {

    /// 当前账号 + 桶是否已在「文件」中挂载
    static func isMounted(sessionId: UUID, accountId: String, bucketName: String) async -> Bool {
        let target = FileProviderDomainID.make(sessionId: sessionId, accountId: accountId, bucketName: bucketName)
        let domains = (try? await allDomains()) ?? []
        return domains.contains { $0.identifier.rawValue == target }
    }

    /// 挂载：新增一个 domain（已存在则幂等返回）
    static func mount(sessionId: UUID, accountId: String, bucketName: String) async throws {
        let id = FileProviderDomainID.make(sessionId: sessionId, accountId: accountId, bucketName: bucketName)
        if await isMounted(sessionId: sessionId, accountId: accountId, bucketName: bucketName) { return }
        let domain = NSFileProviderDomain(
            identifier: NSFileProviderDomainIdentifier(rawValue: id),
            displayName: bucketName
        )
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            NSFileProviderManager.add(domain) { error in
                if let error { cont.resume(throwing: error) } else { cont.resume() }
            }
        }
    }

    /// 卸载：移除该 domain（连同系统侧已下载的副本）
    static func unmount(sessionId: UUID, accountId: String, bucketName: String) async throws {
        let id = FileProviderDomainID.make(sessionId: sessionId, accountId: accountId, bucketName: bucketName)
        let domains = try await allDomains()
        guard let domain = domains.first(where: { $0.identifier.rawValue == id }) else { return }
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            NSFileProviderManager.remove(domain) { error in
                if let error { cont.resume(throwing: error) } else { cont.resume() }
            }
        }
    }

    private static func allDomains() async throws -> [NSFileProviderDomain] {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<[NSFileProviderDomain], Error>) in
            NSFileProviderManager.getDomainsWithCompletionHandler { domains, error in
                if let error { cont.resume(throwing: error) } else { cont.resume(returning: domains) }
            }
        }
    }
}
