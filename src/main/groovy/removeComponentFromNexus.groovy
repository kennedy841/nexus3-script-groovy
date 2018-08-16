import groovy.json.JsonOutput
import org.joda.time.DateTime
import org.sonatype.nexus.repository.storage.Asset
import org.sonatype.nexus.repository.storage.Component
import org.sonatype.nexus.repository.storage.StorageFacet

import java.util.function.Consumer

def repoName = 'npm-snapshot';
def numOfDayFromLastUpdate = 200




def startDate = new DateTime().minusDays(numOfDayFromLastUpdate)
log.info("Gathering Asset list for repository: ${repoName} as of startDate: ${startDate}")
def repo = repository.repositoryManager.get(repoName)

if(repo == null){
    throw new RuntimeException("repository not found ${repoName}")
}

StorageFacet storageFacet = repo.facet(StorageFacet)

def tx = storageFacet.txSupplier().get()
def removedComponents = 0
def componentsSize = 0
try {
    tx.begin()

    def bucket = tx.findBucket(repo)

    def allComponents = tx.browseComponents(bucket);

    allComponents.forEach(new Consumer<Component>() {
        @Override
        void accept(Component t) {

            componentsSize++

            log.info(" component ${t.name()} ${t.version()}")
            if (t.lastUpdated() > startDate) {
                log.info(" remove component ${t.name()} ${t.version()}")
                tx.deleteComponent(t)

                removedComponents++

            } else {
                log.info("component is valid updated on ${t.lastUpdated()}")
            }

        }
    })
    tx.commit()

}
finally {
    tx.close()
}

def result = JsonOutput.toJson([
        componentsSize: componentsSize,
        removedComponents: removedComponents,
        since   : startDate.toString(),
        repoName: repoName
])
log.info(result)
return result

